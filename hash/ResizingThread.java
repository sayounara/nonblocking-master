import java.util.Random;

class ResizingThread extends Thread
{
    private int id;
    private Random oprng;
    private ISet set;

    public int numGrow;
    public int numShrink;

    public ResizingThread(int i)
    {
        this.id = i;
        this.oprng = new Random(i);
        this.set = Benchmark.set;
    }

    public void run()
    {
        while (!Benchmark.begin);

        while (!Benchmark.stop) {
            if (oprng.nextInt(2) == 1) {
                if (set.grow()) {
                    numGrow++;
                }
            }
            else {
                if (set.shrink()) {
                    numShrink++;
                }
            }
        }
    }
}
