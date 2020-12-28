package com.github.kimffy24.uow.component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pro.jk.ejoker.common.utils.genericity.TypeRefer;
import pro.jk.ejoker.common.utils.relationship.IRelationshipScalpel;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeRevertUtil;
import pro.jk.ejoker.common.utils.relationship.RelationshipTreeUtil;
import pro.jk.ejoker.common.utils.relationship.SpecialTypeCodec;
import pro.jk.ejoker.common.utils.relationship.SpecialTypeCodecStore;

public class StdConverter {
	
	private SpecialTypeCodecStore<String> specialTypeHandler;
	
	private RelationshipTreeUtil<Map<String, Object>, List<Object>> relationshipTreeUtil;

	private RelationshipTreeRevertUtil<Map<String, Object>, List<Object>> revertRelationshipTreeUitl;

	@SuppressWarnings("unchecked")
	private StdConverter () {
		specialTypeHandler = new SpecialTypeCodecStore<String>()
				.append(BigDecimal.class, new SpecialTypeCodec<BigDecimal, String>(){

					@Override
					public String encode(BigDecimal target) {
						return target.toPlainString();
					}

					@Override
					public BigDecimal decode(String source) {
						return new BigDecimal(source);
					}
					
				})
				.append(BigInteger.class, new SpecialTypeCodec<BigInteger, String>(){

					@Override
					public String encode(BigInteger target) {
						return target.toString();
					}

					@Override
					public BigInteger decode(String source) {
						return new BigInteger(source);
					}
					
				})
				.append(Character.class, new SpecialTypeCodec<Character, String>(){

					@Override
					public String encode(Character target) {
						return "" + (int )target.charValue();
					}

					@Override
					public Character decode(String source) {
						return (char )Integer.parseInt(source);
					}
					
				})
				.append(Date.class, new SpecialTypeCodec<Date, Date>(){
					@Override
					public Date encode(Date target) {
						return target;
					}
					@Override
					public Date decode(Date source) {
						return source;
					}
				})
				;
		
		IRelationshipScalpel<Map<String, Object>, List<Object>> eval = new IRelationshipScalpel<Map<String, Object>, List<Object>>() {

			@Override
			public Map<String, Object> createKeyValueSet() {
				return new HashMap<>();
			}

			@Override
			public List<Object> createValueSet() {
				return new ArrayList<>();
			}
			
			@Override
			public boolean isHas(Map<String, Object> arg0, Object arg1) {
				return arg0.containsKey(arg1);
			}

			@Override
			public void addToKeyValueSet(Map<String, Object> arg0, Object arg1, String arg2) {
				arg0.put(arg2, arg1);
			}

			@Override
			public void addToValueSet(List<Object> arg0, Object arg1) {
				arg0.add(arg1);
			}
			@Override
			public Set<String> getKeySet(Map<String, Object> arg0) {
				return arg0.keySet();
			}

			@Override
			public int getVPSize(List<Object> arg0) {
				return arg0.size();
			}

			@Override
			public Object getFromKeyValeSet(Map<String, Object> arg0, Object arg1) {
				return arg0.get(arg1);
			}

			@Override
			public Object getValue(List<Object> arg0, int arg1) {
				return arg0.get(arg1);
			}

			@Override
			public boolean hasKey(Map<String, Object> arg0, Object arg1) {
				return arg0.containsKey(arg1);
			}
			
		};
		
		relationshipTreeUtil = new RelationshipTreeUtil<>(eval, specialTypeHandler);
		
		revertRelationshipTreeUitl = new RelationshipTreeRevertUtil<>(eval, specialTypeHandler);
		
	}

	public <T> Map<String, Object> convert(T object) {
		return relationshipTreeUtil.getTreeStructure(object);
	}
	
	public <T> T revert(Map<String, Object> dbObject, Class<T> clazz) {
		return revertRelationshipTreeUitl.revert(dbObject, clazz);
	}

	public <T> T revert(Object kvDataSet, TypeRefer<T> typeRef) {
		return revertRelationshipTreeUitl.revert(kvDataSet, typeRef);
	}
	
	private final static StdConverter instance = new StdConverter();
	
	public final static StdConverter getInstance() {
		return instance;
	}
}
