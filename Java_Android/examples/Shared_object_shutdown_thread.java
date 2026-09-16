import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

// A simple class representing the Object being created
class MessageObject {
    private String content;

    public MessageObject(String content) {
        this.content = content;
    }

    public void showMessage() {
        System.out.println("Object State: " + content);
    }
}

public class Main {
    public static void main(String[] args) {
        // 1. Create a "container" in the Main Thread (acting as Object B).
        // It waits to receive the reference of the Object created in the child thread.
        AtomicReference<MessageObject> sharedContainer = new AtomicReference<>();

        // 2. Create a Thread Pool
        ExecutorService executor = Executors.newSingleThreadExecutor();

        System.out.println("--- THREAD STARTED ---");
        executor.execute(() -> {
            // 3. Create Object A INSIDE the child thread.
            // Memory allocation: The actual object is created in the HEAP memory.
            // The variable 'objA' is just a reference stored in this Thread's STACK.
            MessageObject objA = new MessageObject("I'm still alive even though the thread is dead!");
            
            // 4. Share Object A to the outside container.
            // We are copying the memory address (reference) and giving it to the Main Thread.
            sharedContainer.set(objA);
            
            System.out.println("Child Thread: Object A created and shared successfully.");
        });

        // 5. Command the Thread to SHUTDOWN
        executor.shutdown();
        
        // Loop to wait until the Thread is completely terminated
        while (!executor.isTerminated()) {
            // Waiting...
        }
        System.out.println("--- THREAD IS COMPLETELY SHUTDOWN ---");

        // =========================================================================
        // EXPLANATION: WHY DOES THE OBJECT SURVIVE AFTER SHUTDOWN?
        // =========================================================================
        // 1. When the thread shuts down, its STACK memory is destroyed. 
        //    This means the local variable 'objA' is gone forever.
        // 2. HOWEVER, the actual object data was allocated on the global HEAP memory.
        // 3. Because 'sharedContainer' (which lives in the Main Thread) still 
        //    holds a copy of that reference pointing to the HEAP, 
        //    Java's Garbage Collector (GC) sees that the object is still "in use".
        // 4. Therefore, GC does NOT delete it, and the object survives safely.
        // =========================================================================

        // 6. Retrieve and check Object A from the Main Thread
        MessageObject retrievedObj = sharedContainer.get();
        
        if (retrievedObj != null) {
            System.out.print("Main Thread checking: ");
            retrievedObj.showMessage(); // This will print data normally
        }
    }
}
