package com.ageos.AgeOSBackend.service;

import br.com.efi.efisdk.EfiPay;
import br.com.efi.efisdk.exceptions.EfiPayException;
import com.ageos.AgeOSBackend.Pix.Credentials;
import com.ageos.AgeOSBackend.dto.PixChargeRequest;
import com.ageos.AgeOSBackend.dto.UserDTO;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class PixService {

    private String clientId = "Client_Id_cd0cc5f875ad844686c356d041e94c4795b85cc4";

    private String clientSecret = "Client_Secret_db7821c570fa69a6919646dc61105503319fe1ac";
    private UserDTO userDTO;

    public JSONObject pixCreateEVP(){

        Credentials credentials = new Credentials();

        JSONObject options = configuringJsonObject(credentials);

        try {
            EfiPay efi = new EfiPay(options);
            JSONObject response = efi.call("pixCreateEvp", new HashMap<String,String>(), new JSONObject());
            System.out.println(response.toString());
            return response;
        }catch (EfiPayException e){
            System.out.println(e.getError());
            System.out.println(e.getErrorDescription());
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public JSONObject pixCreateCharge(PixChargeRequest pixChargeRequest) {
        Credentials credentials = new Credentials();

        JSONObject options = configuringJsonObject(credentials);


        JSONObject body = new JSONObject();
        body.put("calendario", new JSONObject().put("expiracao", 3600));
        body.put("devedor", new JSONObject().put("cpf", "12345678909").put("nome", "Francisco da Silva"));
        body.put("valor", new JSONObject().put("original", pixChargeRequest.valor()));
        body.put("chave", pixChargeRequest.chave());
        body.put("solicitacaoPagador", "Serviço realizado.");;

        JSONArray infoAdicionais = new JSONArray();
        infoAdicionais.put(new JSONObject().put("nome", "Campo 1").put("valor", "Informação Adicional1 do PSP-Recebedor"));
        infoAdicionais.put(new JSONObject().put("nome", "Campo 2").put("valor", "Informação Adicional2 do PSP-Recebedor"));
        body.put("infoAdicionais", infoAdicionais);

        try {
            EfiPay efi = new EfiPay(options);
            JSONObject response = efi.call("pixCreateImmediateCharge", new HashMap<String, String>(), body);

            int idFromJson = response.getJSONObject("loc").getInt("id");
            pixGenerateQRCode(String.valueOf(idFromJson));


            return response;
        } catch (EfiPayException e) {
            System.out.println(e.getError());
            System.out.println(e.getErrorDescription());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private void pixGenerateQRCode(String id) {
        Credentials credentials = new Credentials();
        JSONObject options = configuringJsonObject(credentials);

        HashMap<String, String> params = new HashMap<>();
        params.put("id", id);

        try {
            EfiPay efi = new EfiPay(options);
            Map<String, Object> response = efi.call("pixGenerateQRCode", params, new HashMap<String, Object>());

            String base64QRCode = (String) response.get("imagemQrcode");

            String directoryPath = "qr_codes";
            File directory = new File(directoryPath);

            if (!directory.exists()) {
                directory.mkdirs();
            }

            File outputfile = new File(directory, "qrCode_" + id + ".png");

            ImageIO.write(ImageIO.read(new ByteArrayInputStream(javax.xml.bind.DatatypeConverter.parseBase64Binary(base64QRCode.split(",")[1]))), "png", outputfile);
            System.out.println("QR Code gerado e salvo em: " + outputfile.getAbsolutePath());

            // (Opcional) Abrir a imagem gerada
            // Desktop desktop = Desktop.getDesktop();
            // desktop.open(outputfile);

        } catch (EfiPayException e) {
            System.out.println("Erro ao gerar QR Code: " + e.getError() + " - " + e.getErrorDescription());
        } catch (Exception e) {
            System.out.println("Erro inesperado na geração do QR Code: " + e.getMessage());
        }
    }


    private JSONObject configuringJsonObject(Credentials credentials) {

        JSONObject options = new JSONObject();
        options.put("client_id", clientId);
        options.put("client_secret", clientSecret);
        options.put("certificate", credentials.getCertificate());
        options.put("sandbox", credentials.isSandbox());

        return options;
    }

}