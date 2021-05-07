package com.github.kimffy24.uow.export.skeleton;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.kimffy24.uow.core.AggregateActionBinder;

import pro.jk.ejoker.common.context.annotation.persistent.PersistentIgnore;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.functional.IFunction1;
import pro.jk.ejoker.common.utils.genericity.GenericDefinedType;
import pro.jk.ejoker.common.utils.genericity.GenericExpression;
import pro.jk.ejoker.common.utils.genericity.GenericExpressionFactory;

public class AbstractAggregateRoot<TID> {

	protected TID id;
	
	protected int version = 0;

	@PersistentIgnore
	private boolean _dirty = false;
	
	@PersistentIgnore
	private Map<String, Object> _originalStatus = null;

	@PersistentIgnore
	protected final Class<TID> _idType;

	@Override
	public String toString() {
		AARParse aarParse = aarParseStore.get(this.getClass());
		if(null == aarParse)
			return super.toString();
		return aarParse._toStringFactor.trigger(this);
	}
	
	protected AbstractAggregateRoot(TID id) {
		this();
		this.id = id;
	}

	protected AbstractAggregateRoot() {
		AARParse aarParse;
		Class<? extends AbstractAggregateRoot<?>> aggrType = (Class<? extends AbstractAggregateRoot<?>> )this.getClass();
		if(null == (aarParse = aarParseStore.get(aggrType))) {
			GenericExpression genericExpress = GenericExpressionFactory.getGenericExpressDirectly(this.getClass());
			GenericExpression parent = genericExpress;
			while(!AbstractAggregateRoot.class.equals(parent.getDeclarePrototype())) {
				parent = parent.getParent();
			}
			GenericDefinedType[] deliveryTypeMetasTableCopy = parent.getChild().genericDefination.getDeliveryTypeMetasTableCopy();
			GenericDefinedType genericT = deliveryTypeMetasTableCopy[0];
			Class<TID> idClass = (Class<TID> )genericT.rawClazz;
			if(null == idClass || !AcceptableIdType.contains(idClass)) {
				throw new RuntimeException(StringUtilx.fmt("Wrong Aggregate Root id type!!! [type: {}]", idClass));
			}
	
			AARParse previous = aarParseStore.putIfAbsent(aggrType, aarParse = new AARParse(idClass, aggr -> {
				String simpleName = aggr.getClass().getSimpleName();
				StringBuilder sb = new StringBuilder();
				sb.append(simpleName);
				sb.append("{");
	
				StringBuilder sbId = new StringBuilder();
				StringBuilder sbVersion = new StringBuilder();
				StringBuilder sbOther = new StringBuilder();
				StringBuilder sbTypeInfo = new StringBuilder();
				genericExpress.forEachFieldExpressionsDeeply((name, gdf) -> {
					try {
						if("_originalStatus".equals(name))
							return;
						StringBuilder sb_;
						if("id".equals(name)) sb_=sbId;
						else if("version".equals(name)) sb_=sbVersion;
						else if(name.startsWith("_")) sb_=sbTypeInfo;
						else sb_=sbOther;
						Object target = gdf.field.get(aggr);
						if(null != target) {
							sb_.append(name);
							sb_.append(": ");
							sb_.append(target);
							sb_.append(", ");
						}
					} catch (IllegalAccessException e) {
						throw new RuntimeException(StringUtilx.fmt("Field is not accessible!!! [fieldName: {}, type: {}]",
								name,
								simpleName));
					}
				});
				sb.append(sbId);
				sb.append(sbVersion);
				sb.append(sbTypeInfo);
				sb.append(sbOther);
				sb.append("}");
				return sb.toString();
			}));
			
			if(null != previous)
				aarParse = previous;
		}

		this._idType = (Class<TID> )aarParse._idType;
		
	}

	public TID getId() {
		return id;
	}

	public int getVersion() {
		return version;
	}
	
	private final static Set<Class<?>> AcceptableIdType;

	static {
		AcceptableIdType = new HashSet<>();
		AcceptableIdType.add(Integer.class);
		AcceptableIdType.add(Short.class);
		AcceptableIdType.add(Long.class);
		AcceptableIdType.add(String.class);
		
		AggregateActionBinder.registerOriginalDictAccessor(
				ar -> ar._originalStatus,
				(ar, d) -> ar._originalStatus = d,
				(ar, i) -> {
					Class<?> idType = ((AbstractAggregateRoot)ar)._idType;
					Object idValue;
					if(idType.equals(i.getClass())) {
						idValue = i;
					} else {
						if(Long.class.equals(idType)) {
							idValue = Long.parseLong(i.toString());
						} else if(Integer.class.equals(idType)) {
							idValue = Integer.parseInt(i.toString());
						} else {
							throw new RuntimeException(StringUtilx.fmt(
									"Unsupport idValue!!! [type: {}, idType: {}, newIdValueType: {}, newIdValue: {}]",
									ar.getClass().getSimpleName(),
									idType.getName(),
									i.getClass().getName(),
									i
									)) ;
						}
					}
					((AbstractAggregateRoot)ar).id = idValue;
				},
				ar -> ar._dirty,
				ar -> ar._dirty = true
				);
	}
	
	private final static class AARParse {

		public final Class<?> _idType;
		public final IFunction1<String, AbstractAggregateRoot<?>> _toStringFactor;
		public AARParse(Class<?> _idType, IFunction1<String, AbstractAggregateRoot<?>> _toStringFactor) {
			this._idType = _idType;
			this._toStringFactor = _toStringFactor;
		}
		
	}
	
	private final static Map<Class<? extends AbstractAggregateRoot<?>>, AARParse> aarParseStore
		= new ConcurrentHashMap<>();
}
