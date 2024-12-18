import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PrimePerformanceNanoTime {
    // Prime checking method (unchanged from previous implementation)
    private static boolean isPrime(int number) {
        if (number <= 1) return false;
        if (number <= 3) return true;
        
        for (int i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) return false;
        }
        return true;
    }

    // Single-threaded prime finder
    public static List<Integer> findPrimesSingleThreaded(int start, int end) {
        List<Integer> primes = new ArrayList<>();
        
        for (int number = start; number <= end; number++) {
            if (isPrime(number)) {
                primes.add(number);
            }
        }
        
        return primes;
    }

    // Multi-threaded prime finder (using ExecutorService)
    public static List<Integer> findPrimesMultiThreaded(int start, int end, int threadCount) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<List<Integer>>> futures = new ArrayList<>();
        
        int rangePerThread = (end - start + 1) / threadCount;
        
        for (int i = 0; i < threadCount; i++) {
            int threadStart = start + i * rangePerThread;
            int threadEnd = (i == threadCount - 1) ? end : threadStart + rangePerThread - 1;
            
            futures.add(executorService.submit(new PrimeFinderTask(threadStart, threadEnd)));
        }
        
        List<Integer> primes = new ArrayList<>();
        for (Future<List<Integer>> future : futures) {
            primes.addAll(future.get());
        }
        
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
        
        return primes;
    }

    // Callable task for finding primes in a specific range
    private static class PrimeFinderTask implements Callable<List<Integer>> {
        private final int start;
        private final int end;

        public PrimeFinderTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public List<Integer> call() {
            List<Integer> threadPrimes = new ArrayList<>();
            
            for (int number = start; number <= end; number++) {
                if (isPrime(number)) {
                    threadPrimes.add(number);
                }
            }
            
            return threadPrimes;
        }
    }

    public static void main(String[] args) throws Exception {
        // Define search range
        int start = 1;
        int end = 1_000_000;

        // Number of times to run the test for more accurate measurement
        int testRuns = 5;

        // Get available processor cores
        int processorCores = Runtime.getRuntime().availableProcessors();

        // Arrays to store execution times
        long[] singleThreadTimes = new long[testRuns];
        long[] multiThreadTimes = new long[testRuns];

        // Perform multiple test runs
        for (int run = 0; run < testRuns; run++) {
            // Single-threaded timing
            long singleThreadStart = System.nanoTime();
            List<Integer> singleThreadPrimes = findPrimesSingleThreaded(start, end);
            long singleThreadTime = System.nanoTime() - singleThreadStart;
            singleThreadTimes[run] = singleThreadTime;

            // Multi-threaded timing
            long multiThreadStart = System.nanoTime();
            List<Integer> multiThreadPrimes = findPrimesMultiThreaded(start, end, processorCores);
            long multiThreadTime = System.nanoTime() - multiThreadStart;
            multiThreadTimes[run] = multiThreadTime;

            // Validate results
            if (run == 0) {
                System.out.println("Total Primes Found (Single-threaded): " + singleThreadPrimes.size());
                System.out.println("Total Primes Found (Multi-threaded): " + multiThreadPrimes.size());
            }
        }

        // Calculate and print average times
        long singleThreadAverage = calculateAverage(singleThreadTimes);
        long multiThreadAverage = calculateAverage(multiThreadTimes);

        System.out.println("\nPerformance Results:");
        System.out.println("Number of Processor Cores: " + processorCores);
        System.out.println("Average Single-threaded Time: " + convertToMilliseconds(singleThreadAverage) + " ms");
        System.out.println("Average Multi-threaded Time: " + convertToMilliseconds(multiThreadAverage) + " ms");
        System.out.println("Speedup: " + String.format("%.2f", (double) singleThreadAverage / multiThreadAverage) + "x");
    }

    // Helper method to calculate average of an array of longs
    private static long calculateAverage(long[] times) {
        long sum = 0;
        for (long time : times) {
            sum += time;
        }
        return sum / times.length;
    }

    // Helper method to convert nanoseconds to milliseconds
    private static long convertToMilliseconds(long nanoSeconds) {
        return nanoSeconds / 1_000_000;
    }
}