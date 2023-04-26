package Client;

public class Utils {
    public static void  clearScreen()
    {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();            
        } catch (Exception e) {
            System.out.println(e);
        }    
    }
}
