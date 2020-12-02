package app.udf;

public interface PythonRunner {

  public Object run_udf_1(String code, Object arg);

  public Object run_udf_2(String code, Object arg1, Object arg2);

}
