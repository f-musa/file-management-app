package Client;

public class Utils {
    public static void  clearScreen()
    {
        try {
        final String os = System.getProperty("os.name");
        
        if (os.contains("Windows"))
        {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor(); 
        }
        else
        {
            Runtime.getRuntime().exec("clear");
        }           
        } catch (Exception e) {
            System.out.println(e);
        }    
    }
}
