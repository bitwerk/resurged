package basic;

import org.resurged.impl.classgen.asm.AsmGenerator;

public class AsmGeneratorDataTypesTest extends AbstractDataTypesTestCase {
	public AsmGeneratorDataTypesTest(){
		configuration.setGenerator(new AsmGenerator());
	}
}
