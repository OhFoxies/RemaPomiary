package com.rejner.remapomiary.generator.constants;

import com.itextpdf.text.Font;

import com.itextpdf.text.pdf.BaseFont;

public class ProFonts {

    public static BaseFont bfRegular;
    public static BaseFont bfBold;
    public static BaseFont bflogo;

    public static Font fontBold14;
    public static Font fontNormal;
    public static Font small;
    public static Font medium;
    public static Font mediumNotBold;
    public static Font fontNormalBold;
    public static Font large;
    public static Font logoNormal;
    public static Font logo;

    static {
        try {
            bfRegular = BaseFont.createFont("assets/fonts/Roboto-Regular.ttf",
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            bfBold = BaseFont.createFont("assets/fonts/Roboto-Bold.ttf",
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            bflogo = BaseFont.createFont("assets/fonts/TT0610M_.TTF",
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED);

            logo = new Font(bflogo, 30, Font.ITALIC);
            logoNormal = new Font(bfRegular, 30);
            fontBold14 = new Font(bfBold, 12, Font.BOLD);
            fontNormal = new Font(bfRegular, 10);
            fontNormalBold = new Font(bfBold, 10, Font.BOLD);
            medium = new Font(bfBold, 8);
            mediumNotBold = new Font(bfRegular, 8);
            small = new Font(bfRegular, 6);
            large = new Font(bfBold, 50);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

