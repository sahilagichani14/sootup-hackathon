package challenge3;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.views.JavaView;

import java.util.ArrayList;
import java.util.Collections;

public class Challenge3 {

    public static void main(String[] args) {
        String pathToJar = "sootuphackathon3.0/src/main/java/challenge3/challenge3.jar";
        AnalysisInputLocation inputLocation = new JavaClassPathAnalysisInputLocation(pathToJar, SourceType.Application, new ArrayList<>());
        JavaView view = new JavaView(inputLocation);
        var mainMethodSignature = view.getIdentifierFactory()
                .getMethodSignature("Challenge3", "main", "void", Collections.singletonList("java.lang.String[]"));
        var mainMethod = view.getMethod(mainMethodSignature).get();
        IntegerConstantValueAnalysis analysis = new IntegerConstantValueAnalysis(mainMethod.getBody().getStmtGraph());
        analysis.execute();
        System.out.println(analysis.printResult());
        //43214748364721474836473451179943433
    }

}