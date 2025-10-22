package com.rejner.remapomiary.generator.constants;

public class BreakerGg {
    public double ia;
    public double za;
    public double in;

    public BreakerGg(double in) {
        this.in = in;
        if (in == 6) {
            ia = 60.72;
            za = 3.79;
        }

        else if (in == 10) {
            ia = 100;
            za = 2.30;
        }


        else if (in == 16) {
            ia = 133.60;
            za = 1.72;
        }


        else if (in == 20) {
            ia = 173.60;
            za = 1.32;
        }

        else if (in == 25) {
            ia = 229;
            za = 1;
        }
    }
}
