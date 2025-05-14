package xyz.agmstudio.neoblock.data;

import net.minecraft.nbt.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class representing an object that can be saved to and loaded from NBT (Named Binary Tag) format.
 * This class provides methods to serialize and deserialize fields annotated with @NBTData.
 */
public abstract class NBTSaveable {
    /**
     * Called just before saving the NBT data. Can be overridden by subclasses to modify the tag.
     *
     * @param tag The CompoundTag to be saved.
     * @return The modified CompoundTag.
     */
    public CompoundTag onSave(CompoundTag tag) {
        return tag;
    }

    /**
     * Called just after loading the NBT data. Can be overridden by subclasses to perform additional actions.
     *
     * @param tag The CompoundTag that was loaded.
     */
    public void onLoad(CompoundTag tag) {}

    /**
     * Saves the fields of the object to a CompoundTag. Only fields annotated with @NBTData are saved.
     *
     * @return A CompoundTag containing the serialized data of the object.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(NBTData.class)) {
                field.setAccessible(true);
                NBTData annotation = field.getAnnotation(NBTData.class);
                String key = annotation.value().isEmpty() ? field.getName() : annotation.value();
                try {
                    Object value = field.get(this);
                    if (value instanceof Integer) tag.putInt(key, (Integer) value);
                    else if (value instanceof Boolean) tag.putBoolean(key, (Boolean) value);
                    else if (value instanceof String) tag.putString(key, (String) value);
                    else if (value instanceof Enum<?>) {
                        try {
                            Method getId = value.getClass().getMethod("getId");
                            int id = (int) getId.invoke(value);
                            tag.putInt(key, id);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to save enum field: " + field.getName(), e);
                        }
                    } else if (value instanceof Map<?, ?> map) {
                        ListTag list = new ListTag();
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            CompoundTag entryTag = new CompoundTag();
                            entryTag.putString("key", entry.getKey().toString());

                            Object val = entry.getValue();
                            if (val instanceof Integer i) entryTag.putInt("value", i);
                            else if (val instanceof Boolean b) entryTag.putBoolean("value", b);
                            else if (val instanceof String s) entryTag.putString("value", s);
                            else if (val instanceof NBTSaveable saveable) entryTag.put("value", saveable.save());

                            list.add(entryTag);
                        }
                        tag.put(key, list);
                    } else if (value instanceof Set<?> set) {
                        ListTag list = new ListTag();
                        for (Object item : set) {
                            if (item instanceof Integer i) list.add(IntTag.valueOf(i));
                            else if (item instanceof Boolean b) list.add(ByteTag.valueOf(b));
                            else if (item instanceof String s) list.add(StringTag.valueOf(s));
                            else if (item instanceof NBTSaveable saveable) list.add(saveable.save());
                        }
                        tag.put(key, list);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to save field: " + field.getName(), e);
                }
            }
        }
        return onSave(tag);
    }

    /**
     * Loads an instance of a class that extends NBTSaveable from a CompoundTag.
     *
     * @param clazz The class type to load.
     * @param tag   The CompoundTag containing the serialized data.
     * @param <T>   The type of the NBTSaveable.
     * @return An instance of the class populated with data from the tag.
     */
    public static <T extends NBTSaveable> T load(Class<T> clazz, CompoundTag tag) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(NBTData.class)) {
                    field.setAccessible(true);
                    NBTData annotation = field.getAnnotation(NBTData.class);
                    String key = annotation.value().isEmpty() ? field.getName() : annotation.value();
                    Class<?> type = field.getType();

                    if (type == int.class || type == Integer.class) field.set(instance, tag.getInt(key));
                    else if (type == boolean.class || type == Boolean.class) field.set(instance, tag.getBoolean(key));
                    else if (type == String.class) field.set(instance, tag.getString(key));
                    else if (type.isEnum()) {
                        try {
                            int id = tag.getInt(key);
                            Method fromId = type.getMethod("fromId", int.class);
                            Object enumValue = fromId.invoke(null, id);
                            field.set(instance, enumValue);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to load enum field: " + field.getName(), e);
                        }
                    } else if (Map.class.isAssignableFrom(type)) {
                        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
                        Map<String, Object> result = new HashMap<>();
                        ParameterizedType pType = (ParameterizedType) field.getGenericType();
                        Class<?> valueType = (Class<?>) pType.getActualTypeArguments()[1];

                        for (Tag t: list) {
                            CompoundTag entry = (CompoundTag) t;
                            String mapKey = entry.getString("key");

                            Object mapValue = null;
                            if (valueType == Integer.class) mapValue = entry.getInt("value");
                            else if (valueType == Boolean.class) mapValue = entry.getBoolean("value");
                            else if (valueType == String.class) mapValue = entry.getString("value");
                            else if (NBTSaveable.class.isAssignableFrom(valueType))
                                //noinspection unchecked
                                mapValue = NBTSaveable.load((Class<? extends NBTSaveable>) valueType, entry.getCompound("value"));

                            result.put(mapKey, mapValue);
                        }
                        field.set(instance, result);
                    } else if (Set.class.isAssignableFrom(type)) {
                        ListTag list = tag.getList(key, tag.get(key) instanceof ListTag l ? l.getElementType() : Tag.TAG_STRING);
                        Set<Object> result = new HashSet<>();

                        ParameterizedType pType = (ParameterizedType) field.getGenericType();
                        Class<?> elemType = (Class<?>) pType.getActualTypeArguments()[0];

                        for (Tag t: list) {
                            Object val = null;
                            if (elemType == Integer.class) val = ((IntTag) t).getAsInt();
                            else if (elemType == String.class) val = t.getAsString();
                            else if (NBTSaveable.class.isAssignableFrom(elemType))
                                //noinspection unchecked
                                val = NBTSaveable.load((Class<? extends NBTSaveable>) elemType, (CompoundTag) t);
                            result.add(val);
                        }
                        field.set(instance, result);
                    }
                }
            }
            instance.onLoad(tag);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load NBT into " + clazz.getSimpleName(), e);
        }
    }

}
