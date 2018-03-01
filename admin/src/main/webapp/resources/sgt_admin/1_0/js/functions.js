/**
 * Will put a loading image to the target HTML id
 * 
 * @param sourceObject -
 *            Button that called it
 * @param targetObject -
 *            The HTML id
 * @author Kevin Guanche Darias
 */
function showLoading(sourceObject, targetObject) {
	$(targetObject).addClass('fa fa-spinner fa-spin fa-3x fa-fw');
	$(sourceObject).attr('disabled', true);
}

/**
 * Due to a strange bug in <h:inputFile> must remove iframe after upload, or
 * will not allow to upload a second file
 * 
 * @param data - JSF special object, with data about the ajax state
 * @author Kevin Guanche Darias
 */
function fixFileUpload(data) {
	if(typeof data.status === 'string'){
		if(data.status === 'success'){
			removeDomElement('JSFFrameId');
		}
	}else{
		var func = alert;
		if(typeof console !== 'undefined'){
			func = console.warn;
		}
		func('JSF no devolvió lo esperado');
	}
}

/**
 * Removes a element from the DOM
 * @param elementId - The id of the element
 */
function removeDomElement(elementId){
	var element = document.getElementById(elementId);
	element.parentNode.removeChild(element);
}