public class Test {

	public int a(int a, int b) {
		try {
			return a / b;
		} catch (Exception e) {
			System.out.println("catch");

		} finally {
			System.out.println("finally");
		}
		return a + b;
	}

	public static void main(String[] args) {

		Test a = new Test();
		System.out.println(a.a(9, 0));

	}

}
