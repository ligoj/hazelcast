package com.hazelcast.simplemap.impl;

import com.hazelcast.config.SimpleMapConfig;
import com.hazelcast.internal.memory.impl.UnsafeUtil;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.serialization.SerializationService;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static java.lang.String.format;

public class SimpleRecordStore {

    private final SimpleMapConfig config;
    private final SerializationService serializationService;
    private final Class valueClass;
    private final Unsafe unsafe = UnsafeUtil.UNSAFE;
    private final long slabPointer;
    private long recordIndex = 0;
    private long recordDataSize;
    private long recordDataOffset;

    public SimpleRecordStore(SimpleMapConfig config, SerializationService serializationService) {
        this.config = config;
        this.valueClass = config.getValueClass();
        this.serializationService = serializationService;
        this.slabPointer = unsafe.allocateMemory(config.getSizeBytesPerPartition());
        initRecordData(valueClass);

        System.out.println("record size:" + recordDataSize);
    }

    public void insert(Data keyData, Data valueData) {
        Object record = serializationService.toObject(valueData);
        if (record.getClass() != valueClass) {
            throw new RuntimeException(format("Expected value of class '%s', but found '%s' ",
                    record.getClass().getName(), valueClass.getClass().getName()));
        }
       // System.out.println("----------");

      //  System.out.println("record to insert:"+record);

        unsafe.copyMemory(
                record,
                recordDataOffset,
                null,
                slabPointer + (recordIndex * recordDataSize),
                recordDataSize);
        recordIndex++;

//        // verifying what has been stored so far.
//        for (int k = 0; k < recordIndex; k++) {
//            // reads the first int for each record
//            System.out.println("#" + k + " " + unsafe.getInt(slabPointer + (k * recordDataSize)));
//        }
    }

    private void initRecordData(Class clazz) {
        long end = 0;
        long minFieldOffset = Long.MAX_VALUE;
        long maxFieldOffset = 0;
        do {
            for (Field f : valueClass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }

                long fieldOffset = unsafe.objectFieldOffset(f);
                if (fieldOffset > maxFieldOffset) {
                    maxFieldOffset = fieldOffset;
                    System.out.println("fieldOffset:" + fieldOffset + " field.name:" + f.getName());
                    end = fieldOffset + fieldSize(f);
                }

                if (fieldOffset < minFieldOffset) {
                    minFieldOffset = fieldOffset;
                }
            }
        } while ((clazz = clazz.getSuperclass()) != null);

        System.out.println("minFieldOffset:" + minFieldOffset);
        System.out.println("maxFieldOffset:" + maxFieldOffset);
        System.out.println("end:" + end);

        this.recordDataSize = end - minFieldOffset;
        this.recordDataOffset = minFieldOffset;
    }

    private int fieldSize(Field field) {
        if (field.getType().equals(Integer.TYPE)) {
            return 4;
        } else if (field.getType().equals(Long.TYPE)) {
            return 8;
        } else if (field.getType().equals(Short.TYPE)) {
            return 2;
        } else if (field.getType().equals(Float.TYPE)) {
            return 4;
        } else if (field.getType().equals(Double.TYPE)) {
            return 8;
        } else if (field.getType().equals(Boolean.TYPE)) {
            return 1;
        } else if (field.getType().equals(Short.TYPE)) {
            return 2;
        } else {
            throw new RuntimeException();
        }
    }
}
