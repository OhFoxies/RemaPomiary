package com.rejner.remapomiary.generator.constants;

import com.itextpdf.text.Font;

import com.itextpdf.text.pdf.BaseFont;

public class ProFonts {

    public static BaseFont bfRegular;
    public static BaseFont bfBold;

    public static Font fontBold14;
    public static Font fontNormal;
    public static Font small;
    public static Font medium;
    public static Font fontNormalBold;
    public static Font large;

    static {
        try {
            bfRegular = BaseFont.createFont("assets/fonts/Roboto-Regular.ttf",
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            bfBold = BaseFont.createFont("assets/fonts/Roboto-Bold.ttf",
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            fontBold14 = new Font(bfBold, 14, Font.BOLD);
            fontNormal = new Font(bfRegular, 12);
            fontNormalBold = new Font(bfBold, 12, Font.BOLD);
            medium = new Font(bfBold, 10);
            small = new Font(bfRegular, 6);
            large = new Font(bfBold, 50);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

