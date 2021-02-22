package app.udf;

public interface PythonRunner {

  public Object run_udf(String code, Object arg);

}
