package challenge1;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.Type;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import java.util.*;
import java.util.stream.Collectors;

public class Challenge1 {

    public static int number_of_methods;
    public static int number_of_privatefields;
    public static int number_of_methods_endswith_s;
    public static int number_of_method_returntype_same_firstparametertype;
    public static int number_of_assignmentstmt;

    public static void main(String[] args) {
        String pathToBinary = "src/test/resources/example/challenge1.jar";
        AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(pathToBinary);
        JavaView view = new JavaView(inputLocation);

        Collection<JavaSootClass> allClasses = view.getClasses().collect(Collectors.toList());
        for(JavaSootClass javaSootClass: allClasses) {
            Set<JavaSootMethod> allmethods_inclass = javaSootClass.getMethods();
            number_of_methods += allmethods_inclass.size();
            Set<JavaSootField> fields = javaSootClass.getFields();
            for (JavaSootField javaSootField: fields){
                if (javaSootField.isPrivate()){
                    number_of_privatefields+=1;
                }
            }
            for(JavaSootMethod javaSootMethod: allmethods_inclass){
               if(javaSootMethod.getName().endsWith("s")){
                 number_of_methods_endswith_s += 1;
               }
                Type returnType = javaSootMethod.getReturnType();
                int parameterCount = javaSootMethod.getParameterCount();
                if (parameterCount >= 1){
                    Type parameterType = javaSootMethod.getParameterTypes().get(0);
                    if (parameterType.equals(returnType)){
                        number_of_method_returntype_same_firstparametertype+=1;
                    }
                }
            }
        }

        JavaClassType classType = view.getIdentifierFactory().getClassType("org.reflections.util.FilterBuilder");
        MethodSignature methodSignature =
                view
                        .getIdentifierFactory()
                        .getMethodSignature(
                                classType, "test", "boolean",
                                Collections.singletonList("java.lang.String"));

        Optional<JavaSootMethod> method = view.getMethod(methodSignature);
        if (method.isPresent()) {
            JavaSootMethod sootMethod = method.get();
            List<Stmt> stmts = sootMethod.getBody().getStmts();
            for (Stmt stmt : stmts) {
                if (stmt instanceof JAssignStmt) {
                    number_of_assignmentstmt +=1;
                }
            }
        } else {
            System.err.println("Method not found");
        }

        System.out.println(number_of_methods);
        System.out.println(number_of_privatefields);
        System.out.println(number_of_methods_endswith_s);
        System.out.println(number_of_method_returntype_same_firstparametertype);
        System.out.println(number_of_assignmentstmt);
    }

}
