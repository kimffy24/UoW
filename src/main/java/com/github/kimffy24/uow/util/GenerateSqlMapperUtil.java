package com.github.kimffy24.uow.util;

import static pro.jk.ejoker.common.system.enhance.StringUtilx.fmt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.kimffy24.uow.annotation.RBind;
import com.github.kimffy24.uow.export.skeleton.AbstractAggregateRoot;
import com.github.kimffy24.uow.util.KeyMapperStore.Item;

import pro.jk.ejoker.common.system.enhance.EachUtilx;

/**
 */
public class GenerateSqlMapperUtil {
    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
//    	KeyMapperStore.trunToCamel();
//        generateSqlMapper(SupportMapper.class, SupportAggregateRoot.class, "zod_support", "id");
    }

    private final static String CreateSqlTpl = "DROP TABLE IF EXISTS `%s`; \r\nCREATE TABLE `%s` ( \r\n%s) ENGINE=INNODB DEFAULT CHARSET=UTF8 AUTO_INCREMENT=1 COMMENT='';\r\n";
    private final static String FieldTpl = "`%s` %s %s COMMENT '%s',\r\n";
    private final static String PkeyTpl = "PRIMARY KEY (%s) \r\n";

    private final static Set<String> NumberTypeSet =
    		new HashSet<>(Arrays.asList("TINYINT", "SMALLINT", "INTEGER", "BIGINT"));

    /**
     * 虽然我想做复合主键的支持，但我又不想做。花大功夫满足小场景 <br />
     * 搁置吧，只支持单个ID
     * @param prototype
     * @param tableName
     * @param keys
     * @throws IOException
     */
    public static void generateSqlMapper(Class<? extends AbstractAggregateRoot<?>> prototype, String tableName, String... keys) throws IOException{
    	
    	RBind annotationRB = prototype.getAnnotation(RBind.class);
    	if(null == annotationRB) {
    		throw new RuntimeException("This AggregateRoot Type has no @RBind info !!!");
    	}
    	
    	Class<?> mapper = annotationRB.value();

        if(null == keys || 0 ==keys.length) {
        	AbstractAggregateRoot<?> newInstance;
        	try {
				newInstance = prototype.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
        	keys = new String[] { newInstance.getIdFieldName() };
        }
        
		Set<String> keys_ = new HashSet<String>();
		for(String k : keys) {
			keys_.add(k);
		}
        
        Set<String> latestKey = new HashSet<>();

		List<Item> columns = KeyMapperStore.getIntance().getAnaResult(prototype);
        
    	{
	        StringBuilder sql = new StringBuilder();
	        if(null == tableName || "".equals(tableName)){
	            // 未传表明默认用类名
	            tableName = prototype.getSimpleName();
	        }
	        
	        {
	        	// 找出id列并前置
		        List<Item> idItem = new ArrayList<>(keys_.size());
		        EachUtilx.eliminate(columns, i -> {
		        	if(keys_.contains(i.key)) {
		        		idItem.add(i);
		        		return true;
		        	}
		        	return false;
		        });
		        
		        List<Item> idItemNewOrder = new ArrayList<>();
		        idItemNewOrder.addAll(idItem);
		        idItemNewOrder.addAll(columns);
		        
		        columns = idItemNewOrder;
	        }
	        
	        String sqlField = "";
	        for(Item c : columns){
	        	
	            String fName = c.key;
	            String sqlName = c.sqlClName;
	            if(keys_.contains(fName)) {
	            	latestKey.add("`" + sqlName + "`");
	            }
	            
	            
	            if(1 ==keys.length
	            		&& "id".equals(fName)
	            		&& NumberTypeSet.contains(c.tableItem.tableType)
	            		) {
	            	sqlField += String.format(FieldTpl, sqlName, c.tableItem.tableType, "NOT NULL AUTO_INCREMENT", "自增主键");
	            } else {
	            	sqlField += String.format(FieldTpl, sqlName, c.tableItem.tableType, c.tableItem.tableAttr, c.tableItem.comment);
	            }
	        }
	        sqlField += String.format(PkeyTpl, String.join(", ", latestKey));
	
	        sql.append(String.format(CreateSqlTpl, tableName, tableName, sqlField));
	        System.out.println(sql);
    	}
    	

		StringBuilder sb2 = new StringBuilder();
		{
			apd(sb2, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			apd(sb2, "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">");
			apd(sb2, fmt("<mapper namespace=\"{}\">", mapper.getName()));
			apd(sb2, "");

			apd(sb2, getSqlColumnList(columns));
			apd(sb2, getResultMap(prototype, columns, keys_));
			apd(sb2, getUOW(prototype, tableName, columns, keys_));

			apd(sb2, "");
			apd(sb2, "</mapper>");
		}

		System.out.println(sb2);
	}

    private static String getSqlColumnList(List<Item> columns) {
		StringBuilder resultMap = new StringBuilder();
		apd(resultMap, 1, "<sql id=\"Base_Column_List\">");

		String src = "";
		for(Item i : columns) {
			src += getTabHead(2);
			src += fmt("`{}`,\r\n", i.sqlClName);
		}
		resultMap.append(src.substring(0, src.length()-3));
		resultMap.append("\r\n");

		apd(resultMap, 1, "</sql>");
		return resultMap.toString();
	}

    private static String getResultMap(Class<?> prototype, List<Item> columns, Set<String> latestKey) {
		StringBuilder resultMap = new StringBuilder();
		apd(resultMap, 1, fmt("<resultMap id=\"BaseResultMap\" type=\"{}\">", prototype.getName()));
		Iterator<Item> iterator = columns.iterator();
		while(iterator.hasNext()) {
			Item next = iterator.next();
			String fieldName = next.key;
			String sqlField = next.sqlClName;
			String jdbcType = next.jdbcType;
			apd(resultMap, 2, fmt("<{} column=\"{}\" jdbcType=\"{}\" property=\"{}\" />",
					(latestKey.contains(fieldName) ? "id" : "result"),
					sqlField,
					jdbcType,
					fieldName
			));
		}
		apd(resultMap, 1, "</resultMap>");
		return resultMap.toString();
	}

	private static String getUOW(Class<?> prototype, String tableName, List<Item> columns, Set<String> latestKey) {
		StringBuilder sb = new StringBuilder();

		String idKey = latestKey.iterator().next();
		Item idItem = columns.stream().filter(i -> idKey.equals(i.key)).findFirst().get();

		String dataSetKey = "data";
		{
			apd(sb, 1, "<select id=\"fetchById\" resultType=\"map\">");
			apd(sb, 2, fmt("SELECT * FROM `{}`", tableName));
			apd(sb, 2, "<where>");
			apd(sb, 3, idItem.buildFilter("="));
			apd(sb, 2, "</where>");
			apd(sb, 1, "</select>");
			apd(sb, "");
		}

		{
			apd(sb, 1, "<select id=\"locateId\" resultType=\"map\">");
			apd(sb, 2, fmt("SELECT `{}`,`version` FROM `{}`", idItem.sqlClName, tableName));
			apd(sb, 2, "<where>");
			columns.forEach(i -> {
				if(idItem.equals(i))
					return;
				if("version".equals(i.key))
					return;
				apd(sb, 3, fmt("<if test=\"{}.{} != null\">AND {}</if>", dataSetKey, i.key, i.buildFilter("=", dataSetKey)));
			});
			apd(sb, 2, "</where>");
			apd(sb, 1, "</select>");
			apd(sb, "");
		}

		{
			apd(sb, 1, "<insert id=\"createNew\" parameterType=\"map\" keyProperty=\"__new_id__\" useGeneratedKeys=\"true\">");
			apd(sb, 2, fmt("INSERT INTO  `{}`(", tableName));
			apd(sb, 3, "<include refid=\"Base_Column_List\" />");
			apd(sb, 2, ") VALUES (");
			int latest = columns.size()-1;
			for(int i=0; i<=latest; i++) {
				if (i==latest) {
					apd(sb, 3, columns.get(i).buildSep());
				} else {
					apd(sb, 3, columns.get(i).buildSep() + ",");
				}
			}
			apd(sb, 2, ")");
			apd(sb, 1, "</insert>");
			apd(sb, "");
		}

		{
			apd(sb, 1, "<update id=\"updateById\">");
			apd(sb, 2, fmt("UPDATE `{}`", tableName));
			apd(sb, 2, "<set>");
			columns.forEach(i -> {
				if(idItem.equals(i))
					return;
				apd(sb, 3, fmt("<if test=\"{}.{} != null\">{},</if>", dataSetKey, i.key, i.buildFilter("=", dataSetKey)));
			});
			apd(sb, 2, "</set>");
			apd(sb, 2, fmt("<where>{}</where>", idItem.buildFilter("=")));
			apd(sb, 1, "</update>");
			apd(sb, "");
		}

		{
			StringBuilder versionFilter = new StringBuilder();
			apd(sb, 1, "<update id=\"updateByIdAndVersion\">");
			apd(sb, 2, fmt("UPDATE `{}`", tableName));
			apd(sb, 2, "<set>");
			apd(sb, 3, "`version` = `version`+1,");
			columns.forEach(i -> {
				if(idItem.equals(i))
					return;
				if("version".equals(i.key)) {
					versionFilter.append(i.buildFilter("="));
					return;
				}
				apd(sb, 3, fmt("<if test=\"{}.{} != null\">{},</if>", dataSetKey, i.key, i.buildFilter("=", dataSetKey)));
			});
			apd(sb, 2, "</set>");
			apd(sb, 2, fmt("<where>AND {} AND {}</where>",
					idItem.buildFilter("="),
					versionFilter.toString()
			));
			apd(sb, 1, "</update>");
			apd(sb, "");
		}
		
		{
			apd(sb, 1, "<delete id=\"removePermanentlyById\">");
			apd(sb, 2, fmt("DELETE FROM `{}`", tableName));
			apd(sb, 2, "<where>");
			apd(sb, 3, idItem.buildFilter("="));
			apd(sb, 2, "</where>");
			apd(sb, 1, "</delete>");
			apd(sb, "");
		}

		{
			apd(sb, 1, "<!--  The above part is the Uow core dependent on, don't change it  -->");
			apd(sb, 1, "<!--  以上部分是UoW模块运作所需要的，请勿修改 -->");
		}

		return sb.toString();
	}

	private final static void apd(StringBuilder sb, int thl, String content) {
    	if(thl > 0)
    		sb.append(getTabHead(thl));
		sb.append(content);
		sb.append("\r\n");
	}

	private final static void apd(StringBuilder sb, String content) {
		apd(sb, 0, content);
	}
    
    private final static String getTabHead(int level) {
    	String h = "";
    	for(int i=0; i<level; i++)
    		h += "    ";
    	return h;
    }
}