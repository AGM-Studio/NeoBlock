package xyz.agmstudio.neoblock.data;

import net.minecraft.nbt.CompoundTag;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

@SuppressWarnings("IfCanBeSwitch")
public interface NBTSaveable {
    default CompoundTag onSave(CompoundTag tag) {
        return tag;
    }

    default void onLoad(CompoundTag tag) {}

    default CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(NBTData.class)) {
                field.setAccessible(true);
                NBTData annotation = field.getAnnotation(NBTData.class);
                String key = annotation.value().isEmpty() ? field.getName() : annotation.value();
                try {
                    putToTag(tag, key, field.get(this));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to save field: " + field.getName(), e);
                }
            }
        }
        return onSave(tag);
    }

    private static void putToTag(CompoundTag tag, String key, Object value) {
        if (value == null) return;
        if (value instanceof NBTSaveable saveable) tag.put(key, saveable.save());
        else if (value instanceof Integer i) tag.putInt(key, i);
        else if (value instanceof Double d) tag.putDouble(key, d);
        else if (value instanceof Boolean b) tag.putBoolean(key, b);
        else if (value instanceof String s) tag.putString(key, s);
        else if (value instanceof Enum<?>) {
            try {
                Method getId = value.getClass().getMethod("getId");
                int id = (int) getId.invoke(value);
                tag.putInt(key, id);
            } catch (Exception e) {
                tag.putString(key, ((Enum<?>) value).name());
            }
        } else {
            throw new RuntimeException("Unsupported value type: " + value.getClass());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static <R> R getFromTag(CompoundTag tag, String key, Class<R> type, Object root) {
        if (NBTSaveable.class.isAssignableFrom(type)) {
            CompoundTag compound = tag.getCompound(key);
            try {
                return (R) NBTSaveable.load((Class<? extends NBTSaveable>) type, compound, root);
            } catch (RuntimeException ignored) {
                return (R) NBTSaveable.load((Class<? extends NBTSaveable>) type, compound);
            }
        }
        else if (type == Integer.class || type == int.class) return (R) (Integer) tag.getInt(key);
        else if (type == Double.class || type == double.class) return (R) (Double) tag.getDouble(key);
        else if (type == Boolean.class || type == boolean.class) return (R) (Boolean) tag.getBoolean(key);
        else if (type == String.class) return (R) tag.getString(key);
        else if (type.isEnum()) {
            try {
                int id = tag.getInt(key);
                Method fromId = type.getMethod("fromId", int.class);
                return (R) fromId.invoke(null, id);
            } catch (Exception e) {
                try {
                    String name = tag.getString(key);
                    return (R) Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), name);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to load enum from tag using both id and name", ex);
                }
            }
        } else {
            NeoBlockMod.LOGGER.error("Unsupported tag type: {}", type);
            return null;
        }
    }


    static <T extends NBTSaveable> T load(Class<T> clazz, CompoundTag tag, Object... args) {
        try {
            Class<?>[] types = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
            T instance = clazz.getDeclaredConstructor(types).newInstance(args);
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(NBTData.class)) {
                    field.setAccessible(true);
                    NBTData annotation = field.getAnnotation(NBTData.class);
                    String key = annotation.value().isEmpty() ? field.getName() : annotation.value();
                    Class<?> type = field.getType();
                    if (type.isEnum()) {
                        try {
                            int id = tag.getInt(key);
                            Method fromId = type.getMethod("fromId", int.class);
                            Object enumValue = fromId.invoke(null, id);
                            field.set(instance, enumValue);
                        } catch (Exception e) {
                            NeoBlockMod.LOGGER.error("Failed to load enum field: {}", field.getName(), e);
                            throw new RuntimeException("Failed to load enum field: " + field.getName(), e);
                        }
                    } else {
                        Object value = getFromTag(tag, key, type, instance);
                        field.set(instance, value);
                    }
                }
            }
            instance.onLoad(tag);
            return instance;
        } catch (AbortException e) {
            NeoBlockMod.LOGGER.error("NBT loading into {} has been aborted by {}", clazz.getSimpleName(), e);
            return null;
        } catch (Exception e) {
            NeoBlockMod.LOGGER.error("Failed to load NBT into {}", clazz.getSimpleName(), e);
            throw new RuntimeException("Failed to load NBT into " + clazz.getSimpleName(), e);
        }
    }

    class AbortException extends RuntimeException {
        public AbortException(String message) {
            super(message);
        }
    }
}
