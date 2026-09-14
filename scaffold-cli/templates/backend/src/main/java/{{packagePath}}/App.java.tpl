package {{packageName}};

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {{projectName}} 的入口类。
 *
 * <p>脚手架只提供最小可运行骨架：业务代码请在 {{packageName}} 下按领域拆分包，
 * 不要把逻辑堆在入口类里。</p>
 */
public final class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private App() {
    }

    /**
     * 程序入口。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        String message = greeting();
        log.info(message);
        System.out.println(message);
    }

    /**
     * 返回欢迎语，作为冒烟测试的断言目标。
     *
     * @return 欢迎语
     */
    public static String greeting() {
        return "Hello from {{projectName}}!";
    }
}
