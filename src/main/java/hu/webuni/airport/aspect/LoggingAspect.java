package hu.webuni.airport.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

	//ha pl. a tranzakció előtt és után is szeretnénk megszakítani a metódushívást,
	//akkor (ProceedingJointPoint pjp) a bemenő paraméter és @Around az annotáció
	//és
	//metódus előtti teendő(k)
	//majd Object retVal = pjp.proceed();-del tovább kell engedni 
	//metódus utáni teendő(k)
	
	//1. ha metóduson van rajta az annotáció || 2. ha osztályon, vagy interfacen van rajta az annotáció
	@Pointcut("@annotation(hu.webuni.airport.aspect.LogCall) || @within(hu.webuni.airport.aspect.LogCall)")
	public void annotationLogCall() {}
	
	//1.* : minden osztály, 2.*:minden metódus, ..:mindegy milyen a bemenő paraméter
	//@Before("execution (* hu.webuni.airport.repository.*.*(..))")
	
	//ennél rugalmasabb megoldás egy saját annotáció és egy pointcut létrehozása
	//majd a Before paraméterébe ezt a pointcut-ot megadni
	@Before("hu.webuni.airport.aspect.LoggingAspect.annotationLogCall()")
	public void logBefore(JoinPoint joinPoint) {
		
//		System.out.println(String.format("A(z) %s metódus meghívódott az %s osztályban!",
//				joinPoint.getSignature(),
//				joinPoint.getTarget().getClass().getName()));
		
		//miután az annotációt már rá lehet tenni olyan metódusra/osztályra is, amelyiknek nincs interface-e,
		//ezért vizsgálni kell, hogy van-e és attól függővé tenni a visszatérési értéket
		Class<? extends Object> clazz = joinPoint.getTarget().getClass();
		Class<?>[] interfaces = clazz.getInterfaces();
		String type = interfaces.length == 0 ? clazz.getName() : interfaces[0].toString();
		
		System.out.format("A(z) %s metódus meghívódott az %s osztályban!%n", joinPoint.getSignature(), type );
		
	}
		
}
