package basic;

import org.resurged.impl.classgen.jdk6.JdkGenerator;

public class JdkGeneratorBasicTest extends AbstractBasicTestCase {
	public JdkGeneratorBasicTest(){
		configuration.setGenerator(new JdkGenerator());
	}
}
