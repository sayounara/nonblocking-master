import java.util.Random;
import java.text.*;
import gnu.getopt.Getopt;

class BenchWFArray {
	public static int THREAD_NUM = 1;
	public static int DURATION = 1000;
	public static int RO_RATIO;
	public static long KEY_RANGE = 2048;
	public static long INIT_SIZE = 1024;
	public static String ALG_NAME = "WFArray";//测试的hash
	public static boolean SANITY_MODE = false;

	public static int LOAD_FACTOR = 1;
	public static int UPDATE = 20;
	public static int print_vals_num = 100;
	public static int pf_vals_num = 1023;
	public static int put, put_explicit = 0;

	public static volatile ISet set = null;
	public static volatile boolean begin = false;
	public static volatile boolean stop = false;

	private static void printHelp() {
//		 String info= "intset -- STM stress test "+
//		 "(linked list)\n"+
//		 "\n"+
//		 "Usage:\n"+
//		 "  intset [options...]\n"+
//		 "\n"+
//		 "Options:\n"+
//		 "  -h, --help\n"+
//		 "        Print this message\n"+
//		 "  -d, --duration <int>\n"+
//		 "        Test duration in milliseconds\n"+
//		 "  -i, --initial-size <int>\n"+
//		 "        Number of elements to insert before test\n"+
//		 "  -n, --num-threads <int>\n"+
//		 "        Number of threads\n"+
//		 "  -r, --range <int>\n"+
//		 "        Range of integer values inserted in set\n"+
//		 "  -u, --update-rate <int>\n"+
//		 "        Percentage of update transactions\n"+
//		 "  -l, --load-factor <int>\n"+
//		 "        Elements per bucket\n"+
//		 "  -p, --put-rate <int>\n"+
//		 "        Percentage of put update transactions (should be less than percentage of updates)\n"+
//		 "  -b, --num-buckets <int>\n"+
//		 "        Number of initial buckets (stronger than -l)\n"+
//		 "  -v, --print-vals <int>\n"+
//		 "        When using detailed profiling, how many values to print.\n"+
//		 "  -f, --val-pf <int>\n"+
//		 "        When using detailed profiling, how many values to keep track of.\n";
//       System.out.println(info);
		System.out.println("  -a     algorithm");
		System.out.println("  -p     thread num");
		System.out.println("  -d     duration");
		System.out.println("  -R     lookup ratio (0~100)");
		System.out.println("  -M     key range");
		System.out.println("  -I     initial size");
		System.out.println("  -c     sanity mode");
	}

	private static boolean ParseArgs(String[] args) {
		Getopt g = new Getopt("", args, "u:n:d:r:i:h");
		int c;
		String arg = null;
		while ((c = g.getopt()) != -1) {
			switch (c) {
			case 'u':
				arg	= g.getOptarg();
				UPDATE = Integer.parseInt(arg);
				break;
			case 'n':
				arg = g.getOptarg();
				THREAD_NUM = Integer.parseInt(arg);
				break;
			case 'd':
				arg = g.getOptarg();
				DURATION = Integer.parseInt(arg);
				break;
			case 'r':
				arg = g.getOptarg();
				KEY_RANGE = Integer.parseInt(arg);
				break;
			case 'i':
				arg = g.getOptarg();
				INIT_SIZE = Integer.parseInt(arg);
				break;
			case 'h':
				printHelp();
				return false;
			default:
				return false;
			}
		}
		return true;
	}

	private static boolean is_power_of_two(long x) {
		return (x & (x - 1)) == 0;
	}

	private static long pow2roundup(long x) {
		if (x == 0)
			return 1;
		--x;
		x |= x >> 1;
		x |= x >> 2;
		x |= x >> 4;
		x |= x >> 8;
		x |= x >> 16;
		return x + 1;
	}

	private static void RunBench(boolean warmup) throws InterruptedException {
		RO_RATIO=100-UPDATE;
		set = new WFArrayHashSet();
		if (!is_power_of_two(INIT_SIZE)) {
			long initial_pow2 = pow2roundup(INIT_SIZE);
			System.out.println(
					"** rounding up initial (to make it power of 2): old: " + INIT_SIZE + " / new: +initial_pow2+\n");
			INIT_SIZE = initial_pow2;
		}
		if (KEY_RANGE < INIT_SIZE) {
			KEY_RANGE = 2 * INIT_SIZE;
		}

		System.out.println(
				"## Initial: " + INIT_SIZE + " / Range: " + KEY_RANGE + " / Load factor: " + LOAD_FACTOR + "\n");

		double kb = INIT_SIZE * 4 / 1024.0;
		double mb = kb / 1024.0;
		System.out.println("Sizeof initial: " + kb + " KB = " + mb + " MB\n");
		if (!is_power_of_two(KEY_RANGE)) {
			long range_pow2 = pow2roundup(KEY_RANGE);
			System.out.println(
					"** rounding up range (to make it power of 2): old: " + KEY_RANGE + " / new: " + range_pow2 + "\n");
			KEY_RANGE = range_pow2;
		}
		Random rng = new Random();
		for (int i = 0; i < INIT_SIZE; i++) {
			while (true) {
				int key = rng.nextInt((int) KEY_RANGE);
				if (set.insert(key, 0))
					break;
			}
		}

		begin = false;
		stop = false;

		BenchOpsThread[] threads = new BenchOpsThread[THREAD_NUM];

		for (int i = 0; i < threads.length; i++) {
			threads[i] = new BenchOpsThread(i);
			threads[i].start();
		}

		// record start time
		long startTime = System.currentTimeMillis();

		// broadcast begin signal
		begin = true;

		// sleep the main thread
		if (warmup)
			Thread.sleep(500);
		else
			Thread.sleep(DURATION);

		// broadcast stop signal
		stop = true;

		// wait until every thread finishes
		for (int i = 0; i < threads.length; i++) {
			threads[i].join();
		}

		// record end time
		long endTime = System.currentTimeMillis();

		// compute elapsed time
		long elapsed = endTime - startTime;

		long totalOps = 0;
		for (int i = 0; i < threads.length; i++) {
			totalOps += threads[i].ops;
		}

		if (!warmup) {
			System.out.print("Throughput(Mops/s): ");
			System.out.println(new DecimalFormat("#.##").format((double) totalOps / (elapsed*1000.00)));
		}

		System.gc();
	}

	public static void main(String[] args) throws InterruptedException {
		if (!ParseArgs(args))
			return;

	
			RunBench(true);
			RunBench(false);
		
	}
}
