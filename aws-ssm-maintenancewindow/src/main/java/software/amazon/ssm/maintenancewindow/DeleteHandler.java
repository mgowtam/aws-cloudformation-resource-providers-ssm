package software.amazon.ssm.maintenancewindow;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DeleteMaintenanceWindowRequest;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.ssm.maintenancewindow.translator.ExceptionTranslator;
import software.amazon.ssm.maintenancewindow.util.ClientBuilder;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    private static final SsmClient SSM_CLIENT = ClientBuilder.getClient();
    private final ExceptionTranslator exceptionTranslator;

    DeleteHandler() {
        this.exceptionTranslator = new ExceptionTranslator();
    }

    /**
     * Used for unit tests.
     *
     * @param exceptionTranslator Used for translating service model exceptions.
     */
    DeleteHandler(final ExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = exceptionTranslator;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log(String.format("Processing DeleteHandler request %s", request));

        final ResourceModel model = request.getDesiredResourceState();

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .build();

        progressEvent.setStatus(OperationStatus.FAILED);

        if (StringUtils.isNullOrEmpty(model.getWindowId())) {
            progressEvent.setErrorCode(HandlerErrorCode.InvalidRequest);
            progressEvent.setMessage("WindowId must be specified to delete a maintenance window.");
            return progressEvent;
        }

        final DeleteMaintenanceWindowRequest deleteMaintenanceWindowRequest = DeleteMaintenanceWindowRequest.builder()
                .windowId(model.getWindowId()).build();

        try {
            proxy.injectCredentialsAndInvokeV2(deleteMaintenanceWindowRequest, SSM_CLIENT::deleteMaintenanceWindow);

            progressEvent.setStatus(OperationStatus.SUCCESS);

        } catch (final Exception e) {
            final BaseHandlerException cfnException = exceptionTranslator
                    .translateFromServiceException(e, deleteMaintenanceWindowRequest);

            logger.log(cfnException.getCause().getMessage());

            throw cfnException;
        }
        return ProgressEvent.defaultSuccessHandler(null);
    }
}
