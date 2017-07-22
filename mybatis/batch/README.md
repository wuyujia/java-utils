[TOC]
my poor English!!!
# Introduction
This is a util of mybatis batch operation,  
include `batch insert` and `batch update`  
Although it's very convenient to use, it also limit to use  
limit: 
* 1: create background
> In my workspace, I use the SpringFramework with Mybatis, and my db is Mysql.
* 2: design
> It's designed to solve the difficult problem of Mybatis batch operation, no matter `insert` or `update`
* 3: bug
> Because it is batch operation, so I don't consider that once mysql don't allow null value, but insert or update a null value. Although I design a property about null value not insert or not update  
> For example:   
> 1. When I want to insert a JavaBean (mapping with mysql fields, JavaBean's fields use hump names and db fields use underline names), no problem. If there is null value in bean's fields, it will be ignored by util when I set a selecttive is true.  
> 2. When I want to insert a list of JavaBean, 
there are some potential pitfalls. Why? Because I use java reflect to get many fields of db fields mapping, if db `not allow null` and there is `null value` in beans, it will failure to execute sql if don't choose `selective`. If I choose `selective`, and values of beans fields are the same with each other.  
e.g User.class has two field `name` and `age`, one of list has name value , and age value is null, the other of list has null value of name, age has normal value, and both values are `not allow null` in db, so what will happen when execute sql? Failure!!!  
So when you plan to use it, you must consider how to promise each bean has the same value structure. And We hope you will help us to improvement the util

# Quick Start
Framework: Spring + Mybatis  
Where to use: The InsertProvider Or UpdateProvider

StrategyMapper
```java
@Mapper
public interface StrategyMapper {
    @InsertProvider(type = StrategyProvider.class, method = "batchInsert")
    Integer batchInsert(@Param("dataList")List<Strategy> dataList);
}
```

StrategyProvider
```java
public class StrategyProvider {
    public String batchInsert(List<Strategy> dataList) {
        BatchInsertSQLBuilder<Strategy> builder = new BatchInsertSQLBuilder<>();
        builder.setInsertTable("ad_strategy");
        builder.setSelective(true);
        for (Strategy data: dataList) {
            builder.setData(data);
        }
        return builder.toString();
    }
}
```

# End
Thanks for Use, Hope receive your Suggestion or Pull Requests!!!  
Excuse my poor English!!!

