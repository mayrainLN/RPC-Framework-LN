package monolithic;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/21 21:23
 * @description :
 */
public class ServiceImplTest implements ServiceTest{
    @Override
    public String reverseEcho(String param){
        return new StringBuilder(param).reverse().toString();
    }
}
