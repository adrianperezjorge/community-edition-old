/**
 * Post component.
 * 
 * @namespace Alfresco
 * @class Alfresco.BlogPost
 */
(function()
{
   /**
    * Post constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.PostView} The new Post instance
    * @constructor
    */
   Alfresco.BlogPost = function(htmlId)
   {
      this.name = "Alfresco.BlogPost";
      this.id = htmlId;
      
      /* Register this component */
      Alfresco.util.ComponentManager.register(this);

      /* Load YUI Components */
      Alfresco.util.YUILoaderHelper.require(["datasource", "json", "connection", "event", "button", "menu", "editor"], this.onComponentsLoaded, this);
      
      return this;
   }
   
   Alfresco.BlogPost.prototype =
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: "",
         
         containerId: "blog",
         
         postId: "",
         
         postRef: "",
         
         /**
          * define which mode is currently used - could this be a 
          * create, view or edit
          */
         mode: ""
      },
   	
      
      /**
       * Set multiple initialization options at once.
       *
       * @method setOptions
       * @param obj {object} Object literal specifying a set of options
       */
      setOptions: function BlogPost_setOptions(obj)
      {
         this.options = YAHOO.lang.merge(this.options, obj);
         return this;
      },
      
      setMessages: function BlogPost_setMessages(obj)
      {
         Alfresco.util.addMessages(obj, this.name);
         return this;
      },
      
      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function BlogPost_onComponentsLoaded()
      {
         YAHOO.util.Event.onContentReady(this.id, this.onReady, this, true);
      },
   
      /**
       * Fired by YUI when parent element is available for scripting.
       * Component initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function BlogPost_onReady()
      { 
         // don't do anything if we don't have a mode
         if ((this.options.mode === null) || (this.options.mode == ""))
         {
            return;
         }
         
         // check whether we are in edit mode, in this case we have to register the
         // form with the html
         if ((this.options.mode == "edit") || (this.options.mode == "create"))
         {
            this._registerPostForm();
         }
         
         
         // only initialize action hooks and roll-over if we have a post
         // otherwise it's just a create form
         if (this.options.postId.length > 0)
         {
             // Hook action events
             var me = this;
             YAHOO.Bubbling.addDefaultAction("action-link-"+this.options.postId, function BlogPostList_filterAction(layer, args)
             {
                var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
                if (owner !== null)
                {
                   var action = owner.className;
                   var target = args[1].target;
                   if (typeof me[action] == "function")
                   {
                      // extract the id from the element
                      var elemId = owner.id.substring((action + "-").length);
                      me[action].call(me, elemId);
                      args[1].stop = true;
                   }
                }
          		 
                return true;
             });
             
             // initialize the mouse over functionality for the list
             this.initMouseOverListeners();
         }
      },
      
      /**
       * Loads the edit post form and displays it instead of the content
       * The div class should have the same name as the above function (onEditNode)
       */
      onEditNode: function BlogPostList_onEditNode(id)
      {
         var url = Alfresco.constants.URL_CONTEXT + "page/blog-postedit?site=" + this.options.siteId + "&container=" + this.options.containerId + "&postId=" + id;
         window.location = url;
      },
      
      /**
       * Deletes a post.
       * The div class which contain the Delete link should have the same name as the above function (onEditNode)
       */
      onDeleteNode: function BlogPost_onDelete(elemId)
      {
         // make an ajax request to delete the post
         // we can directly go to alfresco for this
         Alfresco.util.Ajax.request(
		   {
		      url: Alfresco.constants.PROXY_URI + "blog/post/site/" + this.options.siteId + "/" + this.options.containerId + "/" + this.options.postId,
		      method: "DELETE",
		      responseContentType : "application/json",
		      successCallback:
		      {
		         fn: this._onDeleted,
		         scope: this
		      },
		      failureMessage: this._msg("post.msg.failedDelete")
		   });
      },

      _onDeleted: function BlogPost__onDeleted(response)
      {
         window.location = Alfresco.constants.URL_CONTEXT + "/page/blog-postlist?site=" + this.options.siteId;
      },

      _getPublishingRestUrl: function Blog__getPublishingRestUrl(postId)
      {
          return Alfresco.constants.PROXY_URI + "blog/post/site/" +
                    this.options.siteId + "/" + this.options.containerId + "/" + postId + "/publishing";
      },
      
      onPublishExternal: function Blog_onPublishExternal(id)
      {
         var me = this;
         // make an ajax request to publish the post
         Alfresco.util.Ajax.request(
		   {
		      url: me._getPublishingRestUrl(id),
		      method: "POST",
		      requestContentType : "application/json",
		      responseContentType : "application/json",
		      dataObj:
		      {
		         action : "publish"
		      },
		      successCallback:
		      {
		         fn: this._onPublished,
		         scope: this
		      },
		      failureMessage: "Unable to publish"
		   });
      },
      
      _onPublished: function Blog__onPublished(response)
      {
          Alfresco.util.PopupManager.displayMessage({text: "Published!"});
          location.reload(true);
      },
     
      onUpdateExternal: function Blog_onUpdateExternal(id)
      {
         // make an ajax request to publish the post
         Alfresco.util.Ajax.request(
		   {
		      url: this._getPublishingRestUrl(id),
		      method: "POST",
		      requestContentType : "application/json",
		      responseContentType : "application/json",
		      dataObj:
		      {
		         action : "update"
		      },
		      successCallback:
		      {
		         fn: this._onUpdated,
		         scope: this
		      },
		      failureMessage: "Unable to publish"
		   });
      },

      _onUpdated: function Blog__onUpdated(response)
      {
          Alfresco.util.PopupManager.displayMessage({text: "Updated!"});
          location.reload(true);
      },    

      onUnpublishExternal: function Blog_onUnpublishExternal(id)
      {
         // make an ajax request to publish the post
         Alfresco.util.Ajax.request(
		   {
		      url: this._getPublishingRestUrl(id),
		      method: "POST",
		      requestContentType : "application/json",
		      responseContentType : "application/json",
		      dataObj:
		      {
		         action : "unpublish"
		      },
		      successCallback:
		      {
		         fn: this._onUnpublished,
		         scope: this
		      },
		      failureMessage: "Unable to unpublish"
		   });
      },
      
      _onUnpublished: function Blog__onUnpublished(response)
      {
          Alfresco.util.PopupManager.displayMessage({text: "Unpublished!"});
          location.reload(true);
      },


      /**
       * Updates a div content and makes sure the div is displayed
       */
      updateAndShowDiv: function BlogPost_updateAndShowDiv(divId, newHTML)
      {
          var elem = YAHOO.util.Dom.get(divId);
          elem.innerHTML = newHTML;
	      YAHOO.util.Dom.removeClass(elem, "hidden");
      },
      
      showDiv: function BlogPost_updateAndShowDiv(divId)
      {
          var elem = YAHOO.util.Dom.get(divId);
	      YAHOO.util.Dom.removeClass(elem, "hidden");
      },
      
      hideDiv: function BlogPost_hideDiv(divId)
      {
          var elem = YAHOO.util.Dom.get(divId);
	      YAHOO.util.Dom.addClass(elem, "hidden");          
      },


      /**
       * Registers the form with the html (that should be available in the page)
       * as well as the buttons that are part of the form.
       */
      _registerPostForm: function BlogPost__registerPostForm()
      {
         // register the okButton
         var okButton = new YAHOO.widget.Button(this.id + "-ok-button", {type: "submit"});
         
         // register the cancel button
         var cancelButton = new YAHOO.widget.Button(this.id + "-cancel-button", {type: "button"});
         cancelButton.subscribe("click", this.onFormCancelButtonClick, this, true);
         
         // instantiate the simple editor we use for the form
		 this.editor = new YAHOO.widget.SimpleEditor(this.id + '-content', {
		     height: '300px',
		     width: '538px',
		     dompath: false, //Turns on the bar at the bottom
		     animate: false, //Animates the opening, closing and moving of Editor windows
		     toolbar: {
		        titlebar: false,
		        buttons: [
		            { group: 'textstyle', label: this._msg("post.form.font"),
		                buttons: [
				            { type: 'push', label: 'Bold CTRL + SHIFT + B', value: 'bold' },
				            { type: 'push', label: 'Italic CTRL + SHIFT + I', value: 'italic' },
				            { type: 'push', label: 'Underline CTRL + SHIFT + U', value: 'underline' },
		                    { type: 'separator' },
		                    { type: 'color', label: 'Font Color', value: 'forecolor', disabled: true },
		                    { type: 'color', label: 'Background Color', value: 'backcolor', disabled: true }
		                ]
		            },
		            { type: 'separator' },
				    { group: 'indentlist', label: this._msg("post.form.list"),
				        buttons: [
				            { type: 'push', label: 'Create an Unordered List', value: 'insertunorderedlist' },
				            { type: 'push', label: 'Create an Ordered List', value: 'insertorderedlist' }
				        ]
				    },
				    { type: 'separator' },
				    { group: 'insertitem', label: this._msg("post.form.link"),
				        buttons: [
				            { type: 'push', label: 'HTML Link CTRL + SHIFT + L', value: 'createlink', disabled: true }
				        ]
				    }
		        ]
		    }
		 });
		 this.editor.render();
         
         // create the form that does the validation/submit
         var postForm = new Alfresco.forms.Form(this.id + "-form");
         postForm.setShowSubmitStateDynamically(true);
         postForm.setSubmitElements(okButton);
         postForm.setAJAXSubmit(true,
         {
            successCallback:
            {
               fn: this.onFormSubmitSuccess,
               scope: this
            },
            failureCallback:
            {
               fn: this.onFormSubmitFailure,
               scope: this
            }
         });
         postForm.setSubmitAsJSON(true);
          
         postForm.doBeforeFormSubmit =
        	{
        	   fn: function(form, obj)
        	   {
			        //Put the HTML back into the text area
					this.editor.saveHTML();
        	   },
        	   //obj: myArbitraryObject,
        	   scope: this
         	}
         
         postForm.init();
      },
      
      onFormSubmitSuccess: function BlogPost_onFormSubmitSuccess(response)
      {
         // forward to the post view page
         window.location =  Alfresco.constants.URL_CONTEXT + "page/blog-postview" +
                            "?site=" + this.options.siteId +
                            "&postId=" + response.json.item.name;
      },
      
      onFormSubmitFailure: function BlogPost_onFormSubmitFailure(response)
      {
         Alfresco.util.PopupManager.displayMessage({text: this._msg("post.msg.failedSubmit")});
      },
      
      onFormCancelButtonClick: function(type, args)
      {
         // redirect to the page we came from
         history.go(-1);
      },
      
      
      // mouse hover functionality
      
      /**
       * Attaches a listener to all passed elements.
       */
      _attachRolloverListener: function(elem, mouseOverEventName, mouseOutEventName)
      {  
         var eventElem = elem;
         
         var mouseOverHandler = function(e)
         {
             // find out whether we actually moved inside the 
             if (! e) var e = window.event;
             var relTarg = e.relatedTarget || e.fromElement;
             while (relTarg != null && relTarg != eventElem && relTarg.nodeName != 'BODY') {
                relTarg = relTarg.parentNode
             }
             if (relTarg == eventElem) return;
             
             // the mouse entered the element, fire an event to inform about it
             YAHOO.Bubbling.fire(mouseOverEventName, {event : e, target : eventElem});
         };
         
         var mouseOutHandler = function(e)
         {
             // find out whether we actually moved inside the 
             if (! e) var e = window.event;
             var relTarg = e.relatedTarget || e.toElement;
             while (relTarg != null && relTarg != eventElem && relTarg.nodeName != 'BODY') {
                relTarg = relTarg.parentNode
             }
             if (relTarg == eventElem) return;
             
             // the mouse exited the element, fire an event to inform about it
             YAHOO.Bubbling.fire(mouseOutEventName, {event : e, target : eventElem});
         };
         
         YAHOO.util.Event.addListener(elem, 'mouseover', mouseOverHandler);
         YAHOO.util.Event.addListener(elem, 'mouseout', mouseOutHandler);
      },
      
      firstMouseOverInit: true,
      
      initMouseOverListeners: function BlogPostList_initMouseOverListeners()
      {
         var mouseEnteredBubbleEventName = 'onPostElementMouseEntered';
         var mouseExitedBubbleEventName = 'onPostElementMouseExited';
         var divs = YAHOO.util.Dom.getElementsByClassName('post', 'div');
         for (var x=0; x < divs.length; x++) {
             this._attachRolloverListener(divs[x], mouseEnteredBubbleEventName, mouseExitedBubbleEventName);
         }
         
         if (this.firstMouseOverInit) {
            this.firstMouseOverInit = false;
            // manage mouse hover/exit
            YAHOO.Bubbling.on(mouseEnteredBubbleEventName, this.onPostElementMouseEntered, this);
            YAHOO.Bubbling.on(mouseExitedBubbleEventName, this.onPostElementMouseExited, this);
         }
      },
      
      /** Called when the mouse enters into a list item. */
      onPostElementMouseEntered: function BlogPostList_onListElementMouseEntered(layer, args)
      {
         var elem = args[1].target;
         YAHOO.util.Dom.addClass(elem, 'overNode');
         var editBloc = YAHOO.util.Dom.getElementsByClassName( 'nodeEdit' , null , elem, null );
         YAHOO.util.Dom.addClass(editBloc, 'showEditBloc');
      },
      
      /** Called whenever the mouse exits a list item. */
      onPostElementMouseExited: function BlogPostList_onListElementMouseExited(layer, args)
      {
         var elem = args[1].target;
         YAHOO.util.Dom.removeClass(elem, 'overNode');
         var editBloc = YAHOO.util.Dom.getElementsByClassName( 'nodeEdit' , null , elem , null );
         YAHOO.util.Dom.removeClass(editBloc, 'showEditBloc');
      },

   
      /**
       * PRIVATE FUNCTIONS
       */

      /**
       * Gets a custom message
       *
       * @method _msg
       * @param messageId {string} The messageId to retrieve
       * @return {string} The custom message
       * @private
       */
      _msg: function BlogPost_msg(messageId)
      {
         return Alfresco.util.message.call(this, messageId, "Alfresco.BlogPost", Array.prototype.slice.call(arguments).slice(1));
      }
   };
})();
