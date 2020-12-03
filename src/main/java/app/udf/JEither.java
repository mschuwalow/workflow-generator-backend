package app.udf;

public class JEither {
  public Object left;
  public Object right;

  public JEither(Object left, Object right) {
    this.left = left;
    this.right = right;
  }

  public boolean isLeft() {
    return this.left != null;
  }

  public boolean isRight() {
    return this.right != null;
  }

}
