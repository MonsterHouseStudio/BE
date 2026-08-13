package com.monsterhouse.storage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 테스트용 실제 이미지 생성기.
 *
 * 바이트 배열을 손으로 만들면 ImageIO 가 디코딩하지 못해
 * "위장 파일 거부" 로직에 걸려버립니다. 진짜 PNG 를 만들어야 합니다.
 */
final class TestImages {

    private TestImages() {
    }

    static byte[] png(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(139, 10, 10));
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
