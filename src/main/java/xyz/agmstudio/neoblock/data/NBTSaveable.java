package xyz.agmstudio.neoblock.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import xyz.agmstudio.neoblock.NeoBlockMod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        else if (value instanceof BlockPos p) tag.putLong(key, p.asLong());
        else if (value instanceof Integer i) tag.putInt(key, i);
        else if (value instanceof Long l) tag.putLong(key, l);
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
        } else if (value instanceof List<?> list) {
            ListTag listTag = new ListTag();
            for (Object item : list) {
                if (item instanceof NBTSaveable saveable) listTag.add(saveable.save());
                else if (item instanceof BlockPos p) listTag.add(LongTag.valueOf(p.asLong()));
                else if (item instanceof Integer i) listTag.add(IntTag.valueOf(i));
                else if (item instanceof Long l) listTag.add(LongTag.valueOf(l));
                else if (item instanceof Double d) listTag.add(DoubleTag.valueOf(d));
                else if (item instanceof Boolean b) listTag.add(ByteTag.valueOf((byte) (b ? 1 : 0)));
                else if (item instanceof String s) listTag.add(StringTag.valueOf(s));
                else throw new RuntimeException("Unsupported list element type: " + item.getClass());
            }
            tag.put(key, listTag);
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
        else if (type == BlockPos.class) return (R) BlockPos.of(tag.getLong(key));
        else if (type == Integer.class || type == int.class) return (R) (Integer) tag.getInt(key);
        else if (type == Long.class || type == long.class) return (R) (Long) tag.getLong(key);
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
        } else if (List.class.isAssignableFrom(type)) {
            Field field = Arrays.stream(root.getClass().getDeclaredFields()).filter(f -> f.getName().equals(key)).findFirst().orElse(null);
            if (field == null) return null;

            ParameterizedType listType = (ParameterizedType) field.getGenericType();
            Class<?> elementType = (Class<?>) listType.getActualTypeArguments()[0];

            List<Object> list = new ArrayList<>();
            ListTag listTag = tag.getList(key, Tag.TAG_COMPOUND);
            for (Tag t : listTag) {
                if (NBTSaveable.class.isAssignableFrom(elementType))
                    list.add(NBTSaveable.load((Class<? extends NBTSaveable>) elementType, (CompoundTag) t, root));
                else if (elementType == BlockPos.class) list.add(BlockPos.of(((LongTag) t).getAsLong()));
                else if (elementType == String.class) list.add(t.getAsString());
                else if (elementType == Integer.class) list.add(((IntTag) t).getAsInt());
                else if (elementType == Long.class) list.add(((LongTag) t).getAsLong());
                else if (elementType == Double.class) list.add(((DoubleTag) t).getAsDouble());
                else if (elementType == Boolean.class) list.add(((ByteTag) t).getAsByte() != 0);
                else throw new RuntimeException("Unsupported list element type: " + elementType);
            }
            return (R) list;
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
