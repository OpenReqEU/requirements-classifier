package com.example.mahout.controller;

import com.example.mahout.entity.*;
import com.example.mahout.service.ClassificationService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/upc/classifier-component/multiclassifier")
@Api(value = "Facade", produces = MediaType.APPLICATION_JSON_VALUE)
public class MultilabelClassificationController {

    @Autowired
    private ClassificationService classificationService;

    @PostMapping("/model")
    @ApiOperation(value = "Create multiple models",
            notes = "Given a list of requirements and a company name, multiple models are created based on the values of a given *property* " +
                    "which are set as a *requirementParts* field. Given a *requirementPart* object:\n\n" +
                    "- *id*: the *property* of the classifier (i.e. *reqDomains*)\n" +
                    "- *text*: a \\n separated list with the values of the *requirementPart* (i.e. the *property*)\n\n" +
                    "This method results in a creation of a set of models, which are created as follows:\n\n" +
                    "- If *modelList* is neither null nor empty, a model is created per each value of *property* in *modelList*\n" +
                    "- Else if *modelList* is null or empty, a model is created per each possible value of *property* found in the dataset\n\n" +
                    " Each model is a sub-classifier evaluating whether a given requirement can be classified as a specific value of " +
                    "the *property* field.\n\n" +
                    "This method is executed in an asynchronous way. As a response you will get an object with a single attribute " +
                    "id (check swagger response below) when the training has started. Once the execution is finished, you will " +
                    "get a response to the endpoint set in the *url* parameter. The format of the response is as follows: \n\n" +
                    "{\n" +
                    "\t\"message\":\"Response message\",\n" +
                    "\t\"id\": \"1562315038067_409\",\n" +
                    "\t\"code\": 200\n" +
                    "}\n\n" +
                    "The 'id' field can be used to match the synchronous response of the request with the asynchronous " +
                    "response of the response. The 'code' field states the HTTP code of the request.\n\n" +
                    "**WARNING**: if no data is provided for a specific property value, no model will be created")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = ResultId.class)})
    public ResultId test(@ApiParam(value = "Request with the requirements to train", required = true) @RequestBody MultiRequirementList request,
                     @ApiParam(value = "The company to which the model belongs", required = true, example = "UPC") @RequestParam("company") String enterpriseName,
                     @ApiParam(value = "Property of the classifier", required = true, example = "requirement") @RequestParam("property") String property,
                     @ApiParam(value = "List of property values to generate models (if empty, all values are generated)")
                         @RequestParam(value = "modelList", required = false) List<String> modelList,
                     @ApiParam(value = "The endpoint where the result of the operation will be returned")
                         @RequestParam("url") String url) throws Exception {

        return classificationService.trainByDomainAsync(
                new RequirementList(request, property),
                enterpriseName,
                property,
                modelList,
                url);

        //return "Train successful";
    }

    @GetMapping("/model/{property}")
    @ApiOperation(value = "Get models from property", notes = "Given a **company** and a **property**, returns a " +
            "list of all models created for that multi-label property. Notice that if no data was provided for a " +
            "specific value, no model has been created.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = String[].class)})
    public List<String> getMultilabelValues(@ApiParam(value = "The company to which the model belongs", required = true, example = "UPC")
                                                @RequestParam("company") String enterpriseName,
                                            @ApiParam(value = "Property of the classifier", required = true, example = "requirement")
                                            @PathVariable("property") String property) {
        return classificationService.getMultilabelValues(enterpriseName, property);
    }

    @DeleteMapping("/model")
    @ApiOperation(value = "Delete multiple models",
            notes = "Given a **company** and a **property**, deletes the associated stored models for the given *property* as follows:\n\n" +
                    "- If *modelList* is neither null nor empty, a model is deleted per each value of *property* in *modelList*\n\n" +
                    "- Else if *modelList* is null or empty, all models of the given *property* are deleted")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = String.class)})
    public ResponseEntity delete(@ApiParam(value = "Property of the classifier (requirement_type)", required = true, example = "requirement") @RequestParam("property") String property,
                                 @ApiParam(value = "Proprietary company of the model", required = true, example = "UPC") @RequestParam("company") String enterpriseName,
                       @ApiParam(value = "List of property values to generate models (if empty, all values are generated)")
                       @RequestParam(value = "modelList", required = false) List<String> modelList) throws Exception {
        CompanyPropertyKey request = new CompanyPropertyKey(enterpriseName, property);
        String msg = classificationService.deleteMulti(request, modelList);
        return new ResponseEntity<>(msg, msg.equals("Model(s) not found") ? HttpStatus.NOT_FOUND : HttpStatus.OK);

    }

    @PutMapping("/model")
    @ApiOperation(value = "Update multiple models",
            notes = "Given a list of requirements and a company name, a multidimensional classifier (i.e. multiple models) is updated based on the values of a given *property* " +
                    "which are set as a *requirementParts* field. Given a *requirementPart* object:\n\n" +
                    "- *id*: the *property* of the classifier (i.e. *reqDomains*)\n" +
                    "- *text*: a \\n separated list with the values of the *requirementPart* (i.e. the *property*)\n\n" +
                    "This method results in the update of a set of models, which are created as follows:\n\n" +
                    "- If *modelList* is neither null nor empty, a model is updated per each value of *property* in *modelList*\n" +
                    "- Else if *modelList* is null or empty, a model is updated per each possible value of *property* found in the dataset\n\n" +
                    " Each model is a sub-classifier evaluating whether a given requirement can be classified as a specific value of " +
                    "the *property* field.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = String.class)})
    public void update(@ApiParam(value = "Request with the requirements to train", required = true) @RequestBody MultiRequirementList request,
                       @ApiParam(value = "The company to which the model belongs", required = true, example = "UPC") @RequestParam("company") String enterpriseName,
                       @ApiParam(value = "Property of the classifier", required = true, example = "requirement") @RequestParam("property") String property,
                       @ApiParam(value = "List of property values to update models (if empty, all values are generated)")
                                   @RequestParam(value = "Model list", required = false) List<String> modelList) throws Exception {
        classificationService.updateMulti(
                new RequirementList(request, property),
                property,
                enterpriseName,
                modelList);
    }

    @PostMapping("/classify")
    @ApiOperation(value = "Classify by domain",
            notes = "Given a list of requirements, a company name and a domain of the company, classifies the list of requirements using" +
                    " the domain model. The result is a list of recommendations based on the classification results.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = RecommendationList.class)})
    public RecommendationList classify(@ApiParam(value = "Request with the requirements to train", required = true) @RequestBody ClassifyRequirementList request,
                                       @ApiParam(value = "The company to which the model belongs", required = true, example = "UPC") @RequestParam("company") String enterpriseName,
                                       @ApiParam(value = "Property of the classifier", required = true, example = "requirement") @RequestParam("property") String property,
                                       @ApiParam(value = "List of property values to generate models (if empty, all values are generated)")
                                           @RequestParam(value = "Model list", required = false) List<String> modelList,
                                       @ApiParam(value = "Whether to apply contextual information analysis or not. If positive, contextual information (i.e. " +
                                               "requirements' position order and hierarchical structure) is used to apply the same " +
                                               "tag during the classification process to all members belonging to a same " +
                                               "document list structure.")
                                           @RequestParam(value = "context", defaultValue = "false", required = false) Boolean context
                         ) throws Exception {
        return classificationService.classifyByDomain(new RequirementList(request),
                enterpriseName,
                property,
                modelList,
                context);
    }

    @RequestMapping(value = "train&test", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Train and test by domain",
            notes = "Returns the result of a k cross-validation using the requirements received in the request and the model" +
                    " of the implicit *company* and *property* (send as parameters in the request). Splits the requirements in k groups, trains a classifier for each group with " +
                    "all of the requirements received except the ones in the group and tests it with the requirements in the group.\n" +
                    "Returns the average of several statistics like the accuracy of the model\n")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = DomainStats.class)})
    public DomainStats trainAndTest(@ApiParam(value = "Request with the requirements to test", required = true) @RequestBody MultiRequirementList request,
                                    @ApiParam(value = "Number of tests", required = true, example = "10") @RequestParam("k") int n,
                                    @ApiParam(value = "Property of the classifier", required = true, example = "requirement") @RequestParam("property") String property,
                                    @ApiParam(value = "List of property values to generate models (if empty, all values are generated)")
                                        @RequestParam(value = "Model list", required = false) List<String> modelList,
                                    @ApiParam(value = "Whether to apply contextual information analysis or not. If positive, contextual information (i.e. " +
                                            "requirements' position order and hierarchical structure) is used to apply the same " +
                                            "tag during the classification process to all members belonging to a same " +
                                            "document list structure.") @RequestParam(value = "context", defaultValue = "false", required = false) Boolean context) throws Exception {
        System.out.println("Starting train and test functionality");
        return classificationService.trainAndTestByDomain(new RequirementList(request, property), n, property, modelList, context);
    }

}
