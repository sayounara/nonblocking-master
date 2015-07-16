import java.util.Random;

class BenchOpsThread extends Thread
{
    private int id;
    private Random oprng;
    private Random keyrng;

    public int ops = 0;

    public BenchOpsThread(int i)
    {
        this.id = i;
        this.oprng = new Random(i);
        this.keyrng = new Random(i);
    }

    public void run()
    {
        int cRatio = Benchmark.RO_RATIO;
        int iRatio = cRatio + (100 - cRatio) / 2;
        ISet set = Benchmark.set;

        while (!Benchmark.begin);

        while (!Benchmark.stop) {
//        	System.out.println("minax");
            int op = oprng.nextInt(100);
            int key = keyrng.nextInt((int)Benchmark.KEY_RANGE);

            if (op < cRatio) {
                set.contains(key);
            }
            else if (op < iRatio) {
                set.insert(key, id);
            }
            else {
                set.remove(key, id);
            }

            ops++;
        }
    }
}
