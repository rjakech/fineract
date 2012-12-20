package org.mifosplatform.portfolio.loanaccount.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.mifosplatform.commands.service.PortfolioCommandSourceWritePlatformService;
import org.mifosplatform.infrastructure.core.api.ApiRequestParameterHelper;
import org.mifosplatform.infrastructure.core.data.EntityIdentifier;
import org.mifosplatform.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.mifosplatform.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.mifosplatform.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.mifosplatform.infrastructure.security.service.PlatformSecurityContext;
import org.mifosplatform.portfolio.charge.data.ChargeData;
import org.mifosplatform.portfolio.charge.service.ChargeReadPlatformService;
import org.mifosplatform.portfolio.loanaccount.data.LoanChargeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/loans/{loanId}/charges")
@Component
@Scope("singleton")
public class LoanChargesApiResource {

    private final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<String>(Arrays.asList("id", "chargeId", "name", "penalty",
            "chargeTimeType", "dueAsOfDate", "chargeCalculationType", "percentage", "amountPercentageAppliedTo", "currency",
            "amountWaived", "amountWrittenOff", "amountOutstanding", "amountOrPercentage", "amount", "amountPaid", "chargeOptions"));

    private final String resourceNameForPermissions = "LOAN";

    private final PlatformSecurityContext context;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final DefaultToApiJsonSerializer<LoanChargeData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public LoanChargesApiResource(final PlatformSecurityContext context, final ChargeReadPlatformService chargeReadPlatformService,
            final DefaultToApiJsonSerializer<LoanChargeData> toApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.chargeReadPlatformService = chargeReadPlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveNewLoanChargeDetails(@Context final UriInfo uriInfo) {

        context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);

        final boolean feeChargesOnly = false;
        final Collection<ChargeData> chargeOptions = this.chargeReadPlatformService.retrieveLoanApplicableCharges(feeChargesOnly);
        final LoanChargeData loanChargeTemplate = LoanChargeData.template(chargeOptions);

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, loanChargeTemplate, RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{loanChargeId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveLoanCharge(@PathParam("loanId") final Long loanId, @PathParam("loanChargeId") final Long loanChargeId,
            @Context final UriInfo uriInfo) {

        context.authenticatedUser().validateHasReadPermission(resourceNameForPermissions);

        final LoanChargeData loanCharge = this.chargeReadPlatformService.retrieveLoanChargeDetails(loanChargeId, loanId);

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, loanCharge, RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String addLoanCharge(@PathParam("loanId") final Long loanId, final String apiRequestBodyAsJson) {

        final EntityIdentifier result = this.commandsSourceWritePlatformService.logCommandSource("CREATE", "LOANCHARGE", "N/A", "loans",
                loanId, "charges", null, apiRequestBodyAsJson);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{loanChargeId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String updateLoanCharge(@PathParam("loanId") final Long loanId, @PathParam("loanChargeId") final Long loanChargeId,
            final String apiRequestBodyAsJson) {

        final EntityIdentifier result = this.commandsSourceWritePlatformService.logCommandSource("UPDATE", "LOANCHARGE", "N/A", "loans",
                loanId, "charges", loanChargeId, apiRequestBodyAsJson);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("{chargeId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String waiveLoanCharge(@PathParam("loanId") final Long loanId, @PathParam("chargeId") final Long loanChargeId,
            @QueryParam("command") final String commandParam) {

        String json = "";
        if (is(commandParam, "waive")) {
            final EntityIdentifier result = this.commandsSourceWritePlatformService.logCommandSource("WAIVE", "LOANCHARGE", "N/A", "loans",
                    loanId, "charges", loanChargeId, "{}");

            json = this.toApiJsonSerializer.serialize(result);
        } else {
            throw new UnrecognizedQueryParamException("command", commandParam);
        }

        return json;
    }

    @DELETE
    @Path("{chargeId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String deleteLoanCharge(@PathParam("loanId") final Long loanId, @PathParam("chargeId") final Long loanChargeId) {

        final EntityIdentifier result = this.commandsSourceWritePlatformService.logCommandSource("DELETE", "LOANCHARGE", "N/A", "loans",
                loanId, "charges", loanChargeId, "{}");

        return this.toApiJsonSerializer.serialize(result);
    }
}