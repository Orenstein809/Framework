package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 🎯 Indique que l'annotation s'applique sur des CLASSES (Type)
@Target(ElementType.TYPE) 
// ⏳ Indique que l'annotation doit être visible à l'EXÉCUTION (Runtime) via la Réflexion
@Retention(RetentionPolicy.RUNTIME) 
public @interface Controller {
    // L'annotation est vide, elle sert juste de marqueur
}