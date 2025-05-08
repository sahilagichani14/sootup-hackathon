package challenge1;

import sootup.apk.frontend.ApkAnalysisInputLocation;
import sootup.core.jimple.common.stmt.JReturnStmt;
import sootup.core.jimple.common.stmt.JReturnVoidStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.MethodModifier;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Challenge1 {

    public static void main(String[] args) {
        String userDirectory = System.getProperty("user.dir");
        ApkAnalysisInputLocation sootClassApkAnalysisInputLocation =
                new ApkAnalysisInputLocation(Paths.get("sootuphackathon3.0/src/main/java/challenge1/whatsapp.apk"), "", Collections.emptyList());
        // Your code goes here :)
        JavaView view = new JavaView(sootClassApkAnalysisInputLocation);
        List<JavaSootClass> javaSootClasses = view.getClasses().collect(Collectors.toList());

        AtomicInteger countPrivateMethods = new AtomicInteger();
        AtomicInteger countpart2 = new AtomicInteger();
        AtomicInteger countpart3 = new AtomicInteger();
        AtomicInteger countpart4 = new AtomicInteger();
        AtomicInteger countpart5 = new AtomicInteger();

        javaSootClasses.forEach(cls -> {
            cls.getMethods().forEach(meth -> {

                //part1
                List<MethodModifier> methodModifiers = meth.getModifiers().stream().collect(Collectors.toList());
                methodModifiers.forEach(methodModifier -> {
                    if (methodModifier.equals(MethodModifier.PRIVATE)){
                        countPrivateMethods.addAndGet(1);
                    }
                });

                //part2
                if (meth.getParameterTypes().size()>1){
                    if (meth.getParameterType(1).equals(meth.getReturnType())){
                        countpart2.addAndGet(1);
                    }
                }

                if (!meth.isAbstract()){

                    List<Stmt> stmts = meth.getBody().getStmts();
                    if (stmts.size()==100){
                        countpart3.addAndGet(1);
                    }

                    long count = stmts.stream()
                            .filter(stmt -> {
                                return stmt.toString().contains("return");
                            })
                            .count();

                    if (count > 1) {
                        countpart4.addAndGet(1);
                    }
                }

                if (meth.getSignature().toString().contains("com.aib.senddirectmessagenewdesign.statussaver.StatusViewActivity_Saver$h: void b(java.lang.Boolean)")){
                    countpart5.addAndGet(meth.getBody().getStmts().size());
                }

            });
        });
        System.out.println(countPrivateMethods);
        System.out.println(countpart2);
        System.out.println(countpart3);
        System.out.println(countpart4);
        System.out.println(countpart5);
        //416 926 20 6141 34

    }

}
