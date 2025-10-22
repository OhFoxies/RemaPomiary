package com.rejner.remapomiary.generator.constants;

public class BreakerB {
    public double ia;
    public double in;
    public double za;

    public BreakerB(double in) {
        this.in = in;
        if (in == 6.00) {
            this.ia = 30;
            this.za = 7.67;
        }

        else if (in == 10.00) {
            this.ia = 50;
            this.za = 4.60;
        }

        else if (in == 16.00) {
            this.ia = 80;
            this.za = 2.88;
        }

        else if (in == 20.00) {
            this.ia = 100;
            this.za = 2.30;
        }
        else if (in == 25.00) {
            this.ia = 125;
            this.za = 1.84;
        }

    }
}
