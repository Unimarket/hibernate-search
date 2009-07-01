package org.hibernate.search.util;

import org.hibernate.search.SearchException;
import org.hibernate.util.ReflectHelper;

/**
 * Utility class to load instances of other classes by using a fully qualified name,
 * or from a class type.
 * Uses reflection and throws SearchException(s) with proper descriptions of the error,
 * like the target class is missing a proper constructor, is an interface, is not found...
 * 
 * @author Sanne Grinovero
 */
public class PluginLoader {
	
	/**
	 * Creates an instance of a target class designed by fully qualified name
	 * 
	 * @param <T> matches the type of targetSuperType: defines the return type
	 * @param targetSuperType the return type of the function, the classNameToLoad will be checked
	 * to be assignable to this type.
	 * @param classNameToLoad a fully qualified class name, whose type is assignable to targetSuperType
	 * @param caller the class of the caller, needed for classloading purposes
	 * @param componentDescription a meaningful description of the role the instance will have,
	 * used to enrich error messages to describe the context of the error
	 * @return a new instance of classNameToLoad
	 * @throws SearchException wrapping other error types with a proper error message for all kind of problems, like
	 * classNotFound, missing proper constructor, wrong type, security errors.
	 */
	public static <T> T instanceFromName(Class<T> targetSuperType, String classNameToLoad,
			Class<?> caller, String componentDescription) {
		final Class<?> clazzDef;
		try {
			clazzDef = ReflectHelper.classForName( classNameToLoad, caller );
		} catch (ClassNotFoundException e) {
			throw new SearchException( "Unable to find " + componentDescription +
					" implementation class: " + classNameToLoad, e );
		}
		return instanceFromClass( targetSuperType, clazzDef, componentDescription );
	}
	
	/**
	 * Creates an instance of target class
	 * @param <T> the type of targetSuperType: defines the return type
	 * @param targetSuperType the created instance will be checked to be assignable to this type
	 * @param classToLoad the class to be instantiated
	 * @param componentDescription a role name/description to contextualize error messages
	 * @return a new instance of classToLoad
	 * @throws SearchException wrapping other error types with a proper error message for all kind of problems, like
	 * missing proper constructor, wrong type, security errors.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T instanceFromClass(Class<T> targetSuperType, Class<?> classToLoad, String componentDescription) {
		checkClassType( classToLoad, componentDescription );
		checkHasValidconstructor( classToLoad, componentDescription );
		Object instance;
		try {
			instance =  classToLoad.newInstance();
		}
		catch ( IllegalAccessException e ) {
			throw new SearchException(
					"Unable to instantiate " + componentDescription + " class: " + classToLoad.getName() +
					". Class or constructor is not accessible.", e );
		}
		catch ( InstantiationException e ) {
			throw new SearchException(
					"Unable to instantiate " + componentDescription + " class: " + classToLoad.getName() +
					". Verify it has a no-args public constructor and is not abstract.", e );
		}
		if ( ! targetSuperType.isInstance( instance ) ) {
			// have a proper error message according to interface implementation or subclassing
			if ( targetSuperType.isInterface() ) {
				throw new SearchException(
						"Wrong configuration of " + componentDescription + ": class " + classToLoad.getName()
						+ " does not implement interface " + targetSuperType.getName() );
			}
			else {
				throw new SearchException(
						"Wrong configuration of " + componentDescription + ": class " + classToLoad.getName()
						+ " is not a subtype of " + targetSuperType.getName() );
			}
		}
		else {
			return (T) instance;
		}
	}

	private static void checkClassType(Class<?> classToLoad, String componentDescription) {
		if ( classToLoad.isInterface() ) {
			throw new SearchException( classToLoad.getName() + " defined for component " + componentDescription
					+ " is an interface: implementation required." );
		}
	}

	/**
	 * Verifies if target class has a no-args constructor, and that it is
	 * accessible in current security manager.
	 * @param classToLoad the class type to check
	 * @param componentDescription adds a meaningful description to the type to describe in the
	 * 	exception message
	 */
	public static void checkHasValidconstructor(Class<?> classToLoad, String componentDescription) {
		try {
			classToLoad.getConstructor();
		} catch (SecurityException e) {
			throw new SearchException( classToLoad.getName() + " defined for component " + componentDescription
					+ " could not be instantiated because of a security manager error", e );
		} catch (NoSuchMethodException e) {
			throw new SearchException( classToLoad.getName() + " defined for component " + componentDescription
					+ " is missing a no-arguments constructor" );
		}
	}

}
