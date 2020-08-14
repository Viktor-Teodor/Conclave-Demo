package communication.controllers;

import communication.auxiliary.ContentWrapper;
import communication.services.SendPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;

@Controller
public class CommunicationController {

    @Autowired
    private SendPriceService messengerForEnclave;

    @GetMapping({"","/"})
    public String sendData(Model model){
        model.addAttribute("contentwrapper", new ContentWrapper());

        return "index";
    }

    @PostMapping({"/senddata"})
    public String sendPrice(@ModelAttribute ("contentwrapper") ContentWrapper priceWrapper, Model model) throws IOException {


        try {
            messengerForEnclave.setPrice(priceWrapper.getPrice());
            messengerForEnclave.receiveEnclaveCertificate();
            messengerForEnclave.sendPriceToEnclave();
            double average = messengerForEnclave.receiveAverageFromEnclave();
            model.addAttribute("average", average);
        }
        catch(Error | InterruptedException e){
            model.addAttribute("error", e);
        }

        return "index";
    }


}
