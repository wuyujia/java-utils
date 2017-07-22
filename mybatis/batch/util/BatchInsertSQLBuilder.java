package com.bqmart.common.util;

/** Created by wuyujia on 17/4/5. */

import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 拼接批量插入语句工具类
 * 只支持javaBean, 不支持其他任何java类, 否则报错
 */
public class BatchInsertSQLBuilder<T> {

    // 需要插入的表名
    private String tableName;

    // 记录最长字段对象中包含的属性名称
    private TreeSet<String> fields;

    private boolean selective = true;

    // 待插入的数据
    private List<T> dataList;

    // 最终拼接的SQL语句缓存
    private StringBuffer sb;

    private static final String open = " (";

    private static final String close = ") ";

    private static final String conjunction = ", ";

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
    }

    // 设置表名
    public BatchInsertSQLBuilder setInsertTable(String tableName) {
        Assert.notNull(tableName, "insert table is empty");
        this.tableName = tableName;
        return this;
    }

    // 设置是否为全量更新
    public BatchInsertSQLBuilder setSelective(boolean selective) {
        this.selective = selective;
        return this;
    }

    // 插入数据
    @SuppressWarnings("all")
    public BatchInsertSQLBuilder setData(T obj) {

        Class<?> clazz = obj.getClass();
        // 反射获取javaBean的方法
        Method[] methods = clazz.getMethods();
        boolean flag = false;
        for (Method method : methods) {
            // 如果是get开头, 就是javaBean的get方法
            if (method.getName().startsWith("get") && !method.getName().equalsIgnoreCase("getClass")) {
//                String beanFieldName = getBeanFieldName(method.getName());
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
            throw new IllegalArgumentException("insert table name is null");
        }
        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("field is null");
        }
        sb.append("INSERT INTO " + tableName);
        sb.append(open);
        Iterator<String> iterator = fields.iterator();
        while (iterator.hasNext()) {
            String field = iterator.next();
            sb.append(SQLUtil.humpToUnderline(getBeanFieldName(field)));
            if (iterator.hasNext()) {
                sb.append(conjunction);
            }
        }
        sb.append(close);
        sb.append("VALUES ");
        Iterator<T> iterator1 = dataList.iterator();
        while (iterator1.hasNext()) {
            T t = iterator1.next();
            sb.append(open);
            iterator = fields.iterator();
            while (iterator.hasNext()) {
                String field = iterator.next();
                try {
                    Method method = t.getClass().getMethod(field);
                    try {
                        Object invoke = method.invoke(t);
                        if(invoke != null && invoke instanceof String){
                            String s = invoke.toString();
                            invoke = s.replace("'","\\'");
                        }
                        sb.append(invoke == null ? "null" : "'" + invoke + "'");
                        if (iterator.hasNext()) {
                            sb.append(conjunction);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            sb.append(close);
            if (iterator1.hasNext()) {
                sb.append(conjunction);
            }
        }
        return sb.toString();
    }
}
