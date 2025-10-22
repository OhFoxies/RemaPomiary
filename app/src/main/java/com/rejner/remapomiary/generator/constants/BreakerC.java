package com.rejner.remapomiary.generator.constants;

public class BreakerC {
    public double ia;
    public double za;
    public double in;

    public BreakerC(double in) {
        this.in = in;
        if (in == 6) {
            ia = 60;
            za = 3.83;
        }

        else if (in == 10) {
            ia = 100;
            za = 2.30;
        }


        else if (in == 16) {
            ia = 160;
            za = 1.44;
        }


        else if (in == 20) {
            ia = 200;
            za = 1.15;
        }

        else if (in == 25) {
            ia = 250;
            za = 0.92;
        }
    }
}
