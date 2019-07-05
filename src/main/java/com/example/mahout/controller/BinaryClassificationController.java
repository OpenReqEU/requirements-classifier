package com.example.mahout.controller;

import com.example.mahout.entity.*;
import com.example.mahout.entity.siemens.SiemensRequirementList;
import com.example.mahout.service.ClassificationService;
import com.example.mahout.service.DataService;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/upc/classifier-component")
@Api(value = "MahoutAPI", produces = MediaType.APPLICATION_JSON_VALUE)
public class BinaryClassificationController {

    @Autowired
    private DataService dataService;
    @Autowired
    private ClassificationService classificationService;

    /**
     * Just for testing purposes
     * @return
     * @throws Exception
     */
//    @GetMapping("/test")
    public String test() throws Exception {
        return "OK";
    }

    @PostMapping("model")
    @ApiOperation(value = "Create a model",
            notes = "Given a list of requirements, and a specific company and property, a new model is generated and stored in "
                    + "the database. Each model is identified by a company and a property. Therefore, there is only one model per" +
                    " property and company.\n\n" +
                    "This method is executed in an asynchronous way. As a response you will get an object with a single attribute " +
                    "id (check swagger response below) when the training has started. Once the execution is finished, you will " +
                    "get a response to the endpoint set at the *url* parameter. The format of the response is as follows: \n\n" +
                    "{\n" +
                    "\t\"message\":\"Response message\",\n" +
                    "\t\"id\": \"1562315038067_409\",\n" +
                    "\t\"code\": 200\n" +
                    "}\n\n" +
                    "The 'id' field can be used to match the synchronous response of the request with the asynchronous " +
                    "response of the response. The 'code' field states the HTTP code of the request.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = ResultId.class)})
    public ResultId train(@ApiParam(value = "Request with the requirements to train the model", required = true) @RequestBody RequirementList request,
                               @ApiParam(value = "Property of the classifier (requirement_type)", required = true, example = "requirement") @RequestParam("property") String property,
                               @ApiParam(value = "Proprietary company of the model", required = true, example = "UPC") @RequestParam("company") String enterpriseName,
                          @ApiParam(value = "The endpoint where the result of the operation will be returned")
                              @RequestParam("url") String url) throws Exception {

        return classificationService.trainAsync(request, property, enterpriseName, url);

    }

    @RequestMapping(value = "model", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Update a model",
            notes = "Given a list of requirements, updates the model of the classifier for the given company")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = String.class)})
    public ResponseEntity update(@ApiParam(value = "Request with the requirements to train and update the model", required = true) @RequestBody RequirementList request,
                         @ApiParam(value = "Property of the classifier (requirement_type)", required = true, example = "requirement") @RequestParam("property") String property,
                         @ApiParam(value = "Proprietary company of the model", required = true, example = "UPC") @RequestParam("company") String enterpriseName) throws Exception {

        String msg = classificationService.update(request, property, enterpriseName);
        return new ResponseEntity<>(msg, msg.equals("Update succsesfull") ? HttpStatus.OK : HttpStatus.NOT_FOUND);

    }

    @RequestMapping(value = "model", method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete a model",
            notes = "Given a **company** and a **property**, deletes the associated stored model. Additionally this method allows" +
                    " some variations:\n" +
                    "- If *property* = \"ALL\", removes all models of the given company.\n" +
                    "- If *company* = \"ALL\", removes all models of all companies (used as safe drop database).")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Files deleted correctly", response = String.class),
            @ApiResponse(code = 404, message = "Model(s) not found", response = String.class)})
    public ResponseEntity deleteModel(@RequestBody CompanyPropertyKey request) throws Exception {

        String msg = classificationService.delete(request);
        return new ResponseEntity<>(msg, msg.equals("Model(s) not found") ? HttpStatus.NOT_FOUND : HttpStatus.OK);
    }

    @RequestMapping(value = "classify", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Classify a list of requirements",
            notes = "Given a list of requirements, and using the model stored for the requested company, the requirements are classified " +
            " and a recommended label is returned for each requirement (with a level of confidence)")
    @ApiResponses( value = {@ApiResponse(code = 200, message = "OK", response = RecommendationList.class)})
    public RecommendationList classify(@ApiParam(value = "Request with the requirements to classify", required = true)@RequestBody ClassifyRequirementList request,
                                       @ApiParam(value = "Property of the classifier (requirement_type)", required = true) @RequestParam("property") String property,
                                       @ApiParam(value = "Proprietary company of the model", required = true) @RequestParam("company") String enterpriseName,
                                       @ApiParam(value = "Apply contextual information analysis?") @RequestParam(value = "context", defaultValue = "false", required = false) Boolean context) throws Exception {

        return classificationService.classify(new RequirementList(request), property, enterpriseName, context);

    }

    @RequestMapping(value = "train&test", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Train and test",
            notes = "Returns the result of k cross-validation using the requirements recieved in the request. Splits the requirements in k groups, trains a classifier for each group with all of the requirements recieved except the ones in the group and tests it with the requirements in the group.\n" +
                    "Returns the average of several statistics like the accuracy of the model\n")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Stats.class)})
    public Stats trainAndTest(@ApiParam(value = "Request with the requirements to test", required = true) @RequestBody RequirementList request,
                                     @ApiParam(value = "Property") @RequestParam("property") String property,
                                     @ApiParam(value = "Number of tests", required = true) @RequestParam("k") int n,
                              @ApiParam(value = "Apply contextual information analysis?") @RequestParam(value = "context", defaultValue = "false", required = false) Boolean context) throws Exception {
        System.out.println("Starting train and test functionality");
        /* Parse the body of the request */
//        JSONObject body = new JSONObject(request);

        Stats result = classificationService.trainAndTest(request, property, n, context);

        return result;

    }

    /*@RequestMapping(value = "parse", method = RequestMethod.POST)
    public RequirementList parseSiemensToOpenReq(@RequestBody SiemensRequirementList siemensRequirementList) {
        return dataService.parseSiemensToOpenReq(siemensRequirementList);
    }*/

}
