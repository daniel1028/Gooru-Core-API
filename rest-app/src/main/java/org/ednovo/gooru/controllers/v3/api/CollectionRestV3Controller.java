package org.ednovo.gooru.controllers.v3.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.ednovo.gooru.controllers.BaseController;
import org.ednovo.gooru.core.api.model.ActionResponseDTO;
import org.ednovo.gooru.core.api.model.Collection;
import org.ednovo.gooru.core.api.model.CollectionItem;
import org.ednovo.gooru.core.api.model.RequestMappingUri;
import org.ednovo.gooru.core.api.model.User;
import org.ednovo.gooru.core.constant.ConstantProperties;
import org.ednovo.gooru.core.constant.Constants;
import org.ednovo.gooru.core.constant.GooruOperationConstants;
import org.ednovo.gooru.core.constant.ParameterProperties;
import org.ednovo.gooru.core.security.AuthorizeOperations;
import org.ednovo.gooru.domain.service.collection.CollectionBoService;
import org.ednovo.gooru.domain.service.collection.CopyCollectionService;
import org.ednovo.goorucore.application.serializer.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CollectionRestV3Controller extends BaseController implements ConstantProperties, ParameterProperties {

	@Autowired
	private CollectionBoService collectionBoService;

	@Autowired
	private CopyCollectionService copyCollectionService;

	private static final String INCLUDE_ITEMS = "includeItems";
	
	private static final String INCLUDE_LAST_MODIFIED_USER = "includeLastModifiedUser";

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_ADD })
	@RequestMapping(value = { RequestMappingUri.V3_COLLECTION }, method = RequestMethod.POST)
	public ModelAndView createCollection(@RequestBody final String data, @RequestParam(value = FOLDER_ID, required = false) final String folderId, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		final ActionResponseDTO<Collection> responseDTO = this.getCollectionBoService().createCollection(folderId, user, buildCollection(data));
		if (responseDTO.getErrors().getErrorCount() > 0) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} else {
			response.setStatus(HttpServletResponse.SC_CREATED);
			responseDTO.getModel().setUri(generateUri(request.getRequestURI(), responseDTO.getModel().getGooruOid()));
		}
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		return toModelAndViewWithIoFilter(responseDTO.getModelData(), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_UPDATE })
	@RequestMapping(value = { RequestMappingUri.V3_COLLECTION_ID }, method = RequestMethod.PUT)
	public void updateCollection(@PathVariable(value = ID) final String collectionId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getCollectionBoService().updateCollection(null, collectionId, buildCollection(data), user);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_COPY })
	@RequestMapping(value = { RequestMappingUri.V3_SOURCE_COLLECTION_ID }, method = RequestMethod.POST)
	public ModelAndView copyCollection(@PathVariable(value = ID) final String collectionId, @RequestParam(value = FOLDER_ID, required = false) final String folderId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final User user = (User) request.getAttribute(Constants.USER);
		final Collection collection = this.getCopyCollectionService().copyCollection(folderId, collectionId, user, buildCollection(data));
		collection.setUri(buildUri(RequestMappingUri.V3_COLLECTION, collection.getGooruOid()));
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		return toModelAndViewWithIoFilter(collection, RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_MOVE })
	@RequestMapping(value = { RequestMappingUri.V3_SOURCE_COLLECTION_ID }, method = RequestMethod.PUT)
	public void moveCollection(@PathVariable(value = ID) final String collectionId, @RequestParam(value = FOLDER_ID, required = false) final String folderId, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final User user = (User) request.getAttribute(Constants.USER);
		getCollectionBoService().moveCollection(folderId, collectionId, user);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_ADD })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_RESOURCE }, method = RequestMethod.POST)
	public ModelAndView createResource(@PathVariable(value = ID) final String collectionId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		final ActionResponseDTO<CollectionItem> responseDTO = this.getCollectionBoService().createResource(collectionId, buildCollectionItem(data), user);
		if (responseDTO.getErrors().getErrorCount() > 0) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} else {
			response.setStatus(HttpServletResponse.SC_CREATED);
			responseDTO.getModel().setUri(generateUri(request.getRequestURI(), responseDTO.getModel().getCollectionItemId()));
			responseDTO.getModel().setTitle(responseDTO.getModel().getResource().getTitle());
		}
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		return toModelAndViewWithIoFilter(responseDTO.getModelData(), RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_ADD })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_RESOURCE_ID }, method = RequestMethod.POST)
	public ModelAndView addResource(@PathVariable(value = COLLECTION_ID) final String collectionId, @PathVariable(value = ID) final String resourceId, @RequestParam(value = COLLECTION_ITEM_ID, required= false) final String collectionItemId, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		CollectionItem collectionItem = this.getCollectionBoService().addResource(collectionId, resourceId, collectionItemId,  user);
		collectionItem.setUri(generateUri(StringUtils.substringBeforeLast(request.getRequestURI(), RequestMappingUri.SEPARATOR), collectionItem.getCollectionItemId()));
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		return toModelAndViewWithIoFilter(collectionItem, RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_UPDATE })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_RESOURCE_ID }, method = RequestMethod.PUT)
	public void updateResource(@PathVariable(value = COLLECTION_ID) final String collectionId, @PathVariable(value = ID) final String collectionItemId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getCollectionBoService().updateResource(collectionId, collectionItemId, buildCollectionItem(data), user);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_ADD })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_QUESTION }, method = RequestMethod.POST)
	public ModelAndView createQuestion(@PathVariable(value = ID) final String collectionId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		final CollectionItem collectionItem = this.getCollectionBoService().createQuestion(collectionId, data, user);
		response.setStatus(HttpServletResponse.SC_CREATED);
		collectionItem.setUri(generateUri(request.getRequestURI(), collectionItem.getCollectionItemId()));
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		return toModelAndViewWithIoFilter(collectionItem, RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_ADD })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_QUESTION_ID }, method = RequestMethod.POST)
	public ModelAndView addQuestion(@PathVariable(value = COLLECTION_ID) final String collectionId, @PathVariable(value = ID) final String questionId, @RequestParam(value = COLLECTION_ITEM_ID, required= false) final String collectionItemId, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		CollectionItem collectionItem = this.getCollectionBoService().addQuestion(collectionId, questionId, collectionItemId, user);
		collectionItem.setUri(generateUri(StringUtils.substringBeforeLast(request.getRequestURI(), RequestMappingUri.SEPARATOR), collectionItem.getCollectionItemId()));
		String includes[] = (String[]) ArrayUtils.addAll(CREATE_INCLUDES, ERROR_INCLUDE);
		return toModelAndViewWithIoFilter(collectionItem, RESPONSE_FORMAT_JSON, EXCLUDE_ALL, true, includes);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_UPDATE })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_QUESTION_ID }, method = RequestMethod.PUT)
	public void updateQuestion(@PathVariable(value = COLLECTION_ID) final String collectionId, @PathVariable(value = ID) final String collectionItemId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getCollectionBoService().updateQuestion(collectionId, collectionItemId, data, user);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_UPDATE })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_ITEM_ID }, method = RequestMethod.PUT)
	public void updateCollectionItem(@PathVariable(value = COLLECTION_ID) final String collectionId, @PathVariable(value = ID) final String collectionItemId, @RequestBody final String data, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getCollectionBoService().updateCollectionItem(collectionId, collectionItemId, buildCollectionItem(data), user);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_DELETE })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_ITEM_ID }, method = RequestMethod.DELETE)
	public void deleteCollectionItem(@PathVariable(value = COLLECTION_ID) final String collectionId, @PathVariable(value = ID) final String collectionItemId, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		this.getCollectionBoService().deleteCollectionItem(collectionId, collectionItemId, user.getPartyUid());
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_READ })
	@RequestMapping(value = { RequestMappingUri.V3_COLLECTION_ID }, method = RequestMethod.GET)
	public ModelAndView getCollection(@PathVariable(value = ID) final String collectionId, @RequestParam(value = INCLUDE_ITEMS, required = false, defaultValue = FALSE) final boolean includeItems, @RequestParam(value = INCLUDE_LAST_MODIFIED_USER, required = false, defaultValue = FALSE) final boolean includeLastModifiedUser, final HttpServletRequest request, final HttpServletResponse response) {
		final User user = (User) request.getAttribute(Constants.USER);
		return toModelAndViewWithIoFilter(this.getCollectionBoService().getCollection(collectionId,COLLECTION, user, includeItems, includeLastModifiedUser), RESPONSE_FORMAT_JSON, EXCLUDE_COLLECTION_ITEMS, true, INCLUDE_COLLECTION_ITEMS);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_READ })
	@RequestMapping(value = { RequestMappingUri.ITEM_ID }, method = RequestMethod.GET)
	public ModelAndView getCollectionItems(@PathVariable(value = ID) final String collectionId, @RequestParam(value = OFFSET_FIELD, required = false, defaultValue = "0") int offset, @RequestParam(value = LIMIT_FIELD, required = false, defaultValue = "10") int limit,
			final HttpServletRequest request, final HttpServletResponse response) {
		return toModelAndViewWithIoFilter(this.getCollectionBoService().getCollectionItems(collectionId, limit, offset), RESPONSE_FORMAT_JSON, EXCLUDE_COLLECTION_ITEMS, true, INCLUDE_COLLECTION_ITEMS);
	}

	@AuthorizeOperations(operations = { GooruOperationConstants.OPERATION_SCOLLECTION_READ })
	@RequestMapping(value = { RequestMappingUri.COLLECTION_RESOURCE_ID, RequestMappingUri.COLLECTION_QUESTION_ID }, method = RequestMethod.GET)
	public ModelAndView getResource(@PathVariable(value = COLLECTION_ID) final String collectionId, @PathVariable(value = ID) final String collectionItemId, final HttpServletRequest request, final HttpServletResponse response) {
		return toModelAndViewWithIoFilter(this.getCollectionBoService().getCollectionItem(collectionId, collectionItemId), RESPONSE_FORMAT_JSON, EXCLUDE_COLLECTION_ITEMS, true, INCLUDE_COLLECTION_ITEMS);
	}

	private Collection buildCollection(final String data) {
		return JsonDeserializer.deserialize(data, Collection.class);
	}

	public CollectionBoService getCollectionBoService() {
		return collectionBoService;
	}

	public CopyCollectionService getCopyCollectionService() {
		return copyCollectionService;
	}

	private CollectionItem buildCollectionItem(final String data) {
		return JsonDeserializer.deserialize(data, CollectionItem.class);
	}
}
