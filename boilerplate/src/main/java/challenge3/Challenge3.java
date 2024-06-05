package challenge3;

import heros.InterproceduralCFG;
import sootup.analysis.interprocedural.ifds.JimpleIFDSSolver;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;

public class Challenge3 {

    public static void main(String[] args){
        String pathToJar = "src/test/resources/example/challenge3.jar";
        AnalysisRunner runner = new AnalysisRunner();

        JimpleIFDSSolver<?, InterproceduralCFG<Stmt, SootMethod>> analysis =
                runner.executeStaticAnalysis(pathToJar, "Challenge3");
        String code = runner.getNumTaintedVarsInEachSink(analysis);
        System.out.println("CODE: " + code);
    }

}