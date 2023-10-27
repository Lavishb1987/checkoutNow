package com.checkoutnow.controller;


import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.SetupIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.terminal.ConnectionToken;
import com.stripe.model.terminal.LocationCollection;
import com.stripe.model.terminal.Reader;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.model.terminal.Location;
import com.stripe.param.terminal.ConnectionTokenCreateParams;
import com.stripe.param.terminal.LocationCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/")
@CrossOrigin(origins = "*") // Enable CORS for all endpoints
public class StripeController {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @GetMapping("/")
    public ResponseEntity<String> TestingStripeController(){
        return ResponseEntity.ok("Stripe controller is running");
    }
    @PostMapping("/register_reader")
    public ResponseEntity<String> registerReader(@RequestParam String registration_code,
                                                     @RequestParam String label,
                                                 @RequestParam String location) {
        try {
            Stripe.apiKey = stripeApiKey;
            String validationError = validateApiKey();
            if (validationError != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);

            }
            logInfo("registration_code:"+ registration_code+",label:"+label+",location:"+location);
            logInfo("Creating a reader");
            Reader reader = Reader.create(
                    new HashMap<String, Object>() {{
                        put("registration_code", registration_code);
                        put("label", label);
                        put("location", location);
                    }}
            );

            logInfo("Reader registered: " + reader.getId());

            // Return a simplified response to avoid exposing unnecessary details
            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("reader_id", reader.getId());

            return ResponseEntity.ok(responseMap.toString());
        } catch (StripeException e) {
            logInfo(e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Error registering reader!");

        }
    }

    @PostMapping("/connection_token")
    public ResponseEntity<String> createConnectionToken() {
        try {
            Stripe.apiKey = stripeApiKey;

            String validationError = validateApiKey();
            if (validationError != null) {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("Error", validationError);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);
            }
            ConnectionTokenCreateParams params =
                    ConnectionTokenCreateParams.builder()
                            .setLocation("tml_FKizSQJOj1X6Om")
                            .build();

            RequestOptions requestOptions =
                    RequestOptions.builder()
                            .setStripeAccount("acct_1N0ugiAinOcHvMN3")
                            .build();
            ConnectionToken token = ConnectionToken.create(params, requestOptions);

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("secret", token.getSecret());
            logInfo(token.getSecret());
            return ResponseEntity.ok(responseMap.toString());
        } catch (StripeException e) {

            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("Error", "Error creating connection token!");
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Error creating connection token!");
        }
    }

    @PostMapping("/create_payment_intent")
    public ResponseEntity<String> createPaymentIntent(@RequestParam(required = false) String payment_method_types,
                                                      @RequestParam(required = false, defaultValue = "manual") String capture_method,
                                                      @RequestParam long amount,
                                                      @RequestParam(required = false, defaultValue = "usd") String currency,
                                                      @RequestParam(required = false, defaultValue = "Example PaymentIntent") String description) {
        try {
            Stripe.apiKey = stripeApiKey;

            String validationError = validateApiKey();
            if (validationError != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);
            }

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .addPaymentMethodType(payment_method_types)
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .setAmount(amount)
                    .setCurrency(currency)
                    .setDescription(description);
//                    .setPaymentMethodOptions(new PaymentIntentCreateParams.PaymentMethodOptions.Builder()
//                            .addAll(payment_method_options)  // Add your logic to parse the 'payment_method_options' parameter
//                            .build());

            PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());

            logInfo("PaymentIntent successfully created: " + paymentIntent.getId());

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("intent", paymentIntent.getId());
            responseMap.put("secret", paymentIntent.getClientSecret());

            return ResponseEntity.ok(responseMap.toString());
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Error creating PaymentIntent! ");

        }
    }

    @PostMapping("/capture_payment_intent")
    public ResponseEntity<String> capturePaymentIntent(@RequestParam String payment_intent_id) {
        try {
            Stripe.apiKey = stripeApiKey;

            String validationError = validateApiKey();
            if (validationError != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationError);
                //.body(logInfo(validationError));
            }

            PaymentIntent paymentIntent = PaymentIntent.retrieve(payment_intent_id);
            PaymentIntent capturedPaymentIntent = paymentIntent.capture();

            logInfo("PaymentIntent successfully captured: " + payment_intent_id);

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("intent", capturedPaymentIntent.getId());
            responseMap.put("secret", capturedPaymentIntent.getClientSecret());

            return ResponseEntity.ok(responseMap.toString());
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Error capturing PaymentIntent!");
            //.body(logInfo("Error capturing PaymentIntent! " + e.getMessage()));
        }
    }

    @PostMapping("/cancel_payment_intent")
    public ResponseEntity<String> cancelPaymentIntent(@RequestParam String payment_intent_id) {
        try {
            Stripe.apiKey = stripeApiKey;

//            String validationError = validateApiKey();
//            if (validationError != null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
//                        //.body(logInfo(validationError));
//            }

            PaymentIntent paymentIntent = PaymentIntent.retrieve(payment_intent_id);
            PaymentIntent canceledPaymentIntent = paymentIntent.cancel();

            logInfo("PaymentIntent successfully canceled: " + payment_intent_id);

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("intent", canceledPaymentIntent.getId());
            responseMap.put("secret", canceledPaymentIntent.getClientSecret());

            return ResponseEntity.ok(responseMap.toString());
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("error canceling payment Intent");
            //.body(logInfo("Error canceling PaymentIntent! " + e.getMessage()));
        }
    }

    @PostMapping("/create_setup_intent")
    public ResponseEntity<String> createSetupIntent(@RequestParam(required = false) String[] payment_method_types,
                                                    @RequestParam(required = false) String customer,
                                                    @RequestParam(required = false) String description,
                                                    @RequestParam(required = false) String on_behalf_of) {
        try {
            Stripe.apiKey = stripeApiKey;

//            String validationError = validateApiKey();
//            if (validationError != null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(logInfo(validationError));
//            }

            SetupIntentCreateParams.Builder paramsBuilder = SetupIntentCreateParams.builder();
            //.addPaymentMethodType(SetupIntentCreateParams.);

            if (customer != null && !customer.isEmpty()) {
                paramsBuilder.setCustomer(customer);
            }

            if (description != null && !description.isEmpty()) {
                paramsBuilder.setDescription(description);
            }

            if (on_behalf_of != null && !on_behalf_of.isEmpty()) {
                paramsBuilder.setOnBehalfOf(on_behalf_of);
            }

            SetupIntent setupIntent = SetupIntent.create(paramsBuilder.build());

            logInfo("SetupIntent successfully created: " + setupIntent.getId());

            Map<String, String> responseMap = new HashMap<>();
            responseMap.put("intent", setupIntent.getId());
            responseMap.put("secret", setupIntent.getClientSecret());

            return ResponseEntity.ok(responseMap.toString());
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Error creating subIntent");
            //.body(logInfo("Error creating SetupIntent! " + e.getMessage()));
        }
    }

    @PostMapping("/attach_payment_method_to_customer")
    public ResponseEntity<String> attachPaymentMethodToCustomer(@RequestParam String payment_method_id) {
        try {
            Stripe.apiKey = stripeApiKey;

            Map<String, Object> params = new HashMap<>();
            params.put("customer", "cus_KdqDBWAK4VjrzI");

            PaymentMethod paymentMethod = PaymentMethod.retrieve(payment_method_id);

            // You can choose to return a different response if needed
            return ResponseEntity.ok(paymentMethod.attach(params).toJson());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body("Error attaching PaymentMethod to Customer! " + e.getMessage());
        }
    }

    @PostMapping("/update_payment_intent")
    public ResponseEntity<PaymentIntent> updatePaymentIntent(
            @RequestParam(name = "payment_intent_id", required = true) String paymentIntentId,
            @RequestBody(required = false) Map<String, Object> updateParams
    ) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        PaymentIntent updatedPaymentIntent =
                paymentIntent.update(updateParams);
        return ResponseEntity.ok(updatedPaymentIntent);
    }

    @GetMapping("/list_locations")
    public ResponseEntity<LocationCollection> listLocations() throws StripeException {
        Stripe.apiKey = stripeApiKey;

        Map<String, Object> params = new HashMap<>();
        params.put("limit", 100);

        LocationCollection locations = Location.list(params);
        return ResponseEntity.ok(locations);
    }

    @PostMapping("/create_location")
    public ResponseEntity<Location> createLocation(@RequestParam String displayName, @RequestParam String line1,
                                                   @RequestParam String city,
                                                   @RequestParam String state,
                                                   @RequestParam String country,
                                                   @RequestParam String postalCode) throws StripeException {
        Stripe.apiKey = stripeApiKey;
        try{
            LocationCreateParams.Address address =
                    LocationCreateParams.Address.builder()
                            .setLine1(line1)
                            .setCity(city)
                            .setState(state)
                            .setCountry(country)
                            .setPostalCode(postalCode)
                            .build();
            LocationCreateParams params =
                    LocationCreateParams.builder()
                            .setDisplayName(displayName)
                            .setAddress(address)
                            .build();
            RequestOptions requestOptions =
                    RequestOptions.builder()
                            .setStripeAccount("acct_1N0ugiAinOcHvMN3")
                            .build();
            Location location = Location.create(params, requestOptions);
            return ResponseEntity.ok(location);
        }catch (StripeException e){
            return null;
                    //ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED);
        }


    }

    // Implement other endpoints (capture_payment_intent, cancel_payment_intent, create_setup_intent, etc.)

    private String validateApiKey() {
        if (stripeApiKey == null || stripeApiKey.isEmpty()) {
            return "Error: you provided an empty secret key. Please provide your test mode secret key. For more information, see https://stripe.com/docs/keys";
        }
        if (stripeApiKey.startsWith("pk")) {
            return "Error: you used a publishable key to set up the example backend. Please use your test mode secret key. For more information, see https://stripe.com/docs/keys";
        }
//        if (stripeApiKey.startsWith("sk_live")) {
//            return "Error: you used a live mode secret key to set up the example backend. Please use your test mode secret key. For more information, see https://stripe.com/docs/keys#test-live-modes";
//        }
        return null;
    }
    private void logInfo(String message) {
        System.out.println("\n" + message + "\n\n");
    }

}

