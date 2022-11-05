package hu.webuni.airport.aspect;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
//target-nak be lett állítva a type és a method, hogy akár egész osztályokra (type) és / vagy csak egyes metódusokra is rá lehessen majd tenni  
@Target({ TYPE, METHOD })
public @interface LogCall {
	
}