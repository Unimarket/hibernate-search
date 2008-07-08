// $Id$
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Documented;

/**
 * Defines a FullTextFilter that can be optionally applied to
 * every FullText Queries
 * While not related to a specific indexed entity, the annotation has to be set on one of them
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.TYPE } )
@Documented
public @interface FullTextFilterDef {
	/**
	 * Filter name. Must be unique across all mappings for a given persistence unit
	 */
	String name();

	/**
	 * Either implements org.apache.lucene.search.Filter
	 * or contains a @Factory method returning one.
	 * The Filter generated must be thread-safe
	 *
	 * If the filter accept parameters, an @Key method must be present as well
	 *
	 */
	@SuppressWarnings("unchecked")
	Class impl();

	/**
	 * Enable caching for this filter (default true).
	 */
	boolean cache() default true;
	
	/**
	 * Determines whether the filter should be wrapped around a <code>CachingWrapperFilter</code>.
	 */	
	CachingWrapperFilter useCachingWrapperFilter() default CachingWrapperFilter.AUTOMATIC;
}
