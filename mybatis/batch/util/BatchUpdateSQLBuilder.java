package com.bqmart.common.util;

/** Created by wuyujia on 17/4/5. */

import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 拼接批量更新语句工具类
 * 只支持javaBean, 不支持其他任何java类, 否则报错
 */
public class BatchUpdateSQLBuilder<T> {

    // 需要插入的表名
    private String tableName;

    // 直接指定主键名称
    private String primaryKey;

    // 记录最长字段对象中包含的属性名称
    private TreeSet<String> fields;

    private boolean selective = true;

    // 待插入的数据
    private List<T> dataList;

    private Set primaryValueList;

    // 最终拼接的SQL语句缓存
    private StringBuffer sb;

    private static final String OPEN = " (";

    private static final String CLOSE = ") ";

    private static final String CONJUNCTION = ", ";

    private static final String SET = " SET ";

    private static final String EQUAL = " = ";

    private static final String WHEN = " WHEN ";

    private static final String CASE = " CASE ";

    private static final String THEN = " THEN ";

    private static final String END = " END ";

    private static final String WHERE = " WHERE ";

    private static final String IN = " IN ";

    // 初始化dataList
    {
        if (dataList == null) {
            dataList = new LinkedList<>();
        }
        if (sb == null) {
            sb = new StringBuffer();
        }
        if (fields == null) {
            // 重写目的是为了Set集合能够排序
            fields = new TreeSet<>();
        }
        if (primaryValueList == null) {
            primaryValueList = new HashSet();
        }
    }

    // 设置表名
    public BatchUpdateSQLBuilder setUpdateTable(String tableName, String primaryKey) {
        Assert.notNull(tableName, "update table name is empty");
        Assert.notNull(primaryKey, "primaryKey is empty");
        this.tableName = tableName;
        this.primaryKey = primaryKey;
        return this;
    }

    // 设置是否为全量更新
    public BatchUpdateSQLBuilder setSelective(boolean selective) {
        this.selective = selective;
        return this;
    }

    // 插入数据
    @SuppressWarnings("all")
    public BatchUpdateSQLBuilder setData(T obj) {

        Class<?> clazz = obj.getClass();
        // 反射获取javaBean的方法
        Method[] methods = clazz.getMethods();
        boolean flag = false;
        for (Method method : methods) {
            // 如果是get开头, 就是javaBean的get方法
            if (method.getName().startsWith("get") && !method.getName().equalsIgnoreCase("getClass")) {
                if (selective) {
                    // 记录有值的属性长度
                    try {
                        // 如果方法值不为空, 则将该字段加入到最终映射字段中
                        if (!Objects.isNull(method.invoke(obj))) {
                            if (!flag) {
                                flag = true;
                            }
                            fields.add(method.getName());
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (!flag) {
                        flag = true;
                    }
                    fields.add(method.getName());
                }
            }
        }
        if (flag) {
            // 如果字段不全为空, 则将数据加入到数据集中
            dataList.add(obj);
        }
        return this;
    }

    // 获取Bean 字段的名称, 以get方法为准
    @SuppressWarnings("all")
    private String getBeanFieldName(String getMethodName) {
        String substring = getMethodName.substring(3);
        // 将get字符截取去除之后, 将剩余字符的首字母转换为小写字母
        String substring1 = substring.substring(0, 1);
        String s = substring1.toLowerCase();
        String substring2 = substring.substring(1);
        return s + substring2;
    }

    // 获取批量插入SQL
    @Override
    public String toString() {
        if (!sb.toString().isEmpty()) {
            return sb.toString();
        }
        if (tableName == null || tableName.isEmpty()) {
            throw new IllegalArgumentException("update table name is null");
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("field is null");
        }
        sb.append("UPDATE ").append(tableName).append(SET);

        Iterator<String> it = fields.iterator();
        while (it.hasNext()) {
            String field = it.next();
            sb.append(SQLUtil.humpToUnderline(getBeanFieldName(field))).append(EQUAL);
            sb.append(CASE).append(primaryKey);
            Iterator<T> iterator = dataList.iterator();
            while (iterator.hasNext()) {
                sb.append(WHEN);
                T next = iterator.next();
                Class<?> clazz = next.getClass();
                try {
                    Field primary = clazz.getDeclaredField(SQLUtil.underlineTohump(primaryKey));
                    primary.setAccessible(true);
                    try {
                        Object value = primary.get(next);
                        if(value != null && value instanceof String){
                            String s = value.toString();
                            value = s.replace("'","\\'");
                        }
                        Assert.notNull(value, "primary key value is null");
                        primaryValueList.add(value);
                        try {
                            Method method = clazz.getMethod(field);
                            Object invoke = method.invoke(next);
                            if(invoke != null && invoke instanceof String){
                                String s = invoke.toString();
                                invoke = s.replace("'","\\'");
                            }
                            sb.append("'").append(value).append("'").append(THEN).append(invoke == null ? "null" : "'" + invoke + "'");
                            sb.append("");
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
            sb.append(END);
            if (it.hasNext()) {
                sb.append(CONJUNCTION);
            }
        }

        sb.append(WHERE).append(primaryKey).append(IN).append(OPEN);
        Iterator iterator = primaryValueList.iterator();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            sb.append("'").append(next).append("'");
            if (iterator.hasNext()) {
                sb.append(CONJUNCTION);
            }
        }
        sb.append(CLOSE);

        return sb.toString();
    }
}
