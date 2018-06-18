(defn camel->lisp
  "from https://gist.github.com/idmit/2b444ef94316dad2cd31452d4ab86871"
  [string]
  (-> string
      (clojure.string/replace #"(.)([A-Z][a-z]+)" "$1-$2")
      (clojure.string/replace #"([a-z0-9])([A-Z])" "$1-$2")
      (clojure.string/lower-case)
      ))

(set! *print-length* 500)

(let [all-material-names (->> (file-seq (clojure.java.io/file "node_modules/@material-ui"))
                              (map (fn [it] (.getName it)))
                              (filter (fn [it] ((into #{} "ABCDEFGHIJKLMNOPQRSTUVWXYZ") (first it))))
                              (filter (fn [it] (clojure.string/ends-with? it ".js")))
                              (map (fn [it] (subs it 0 (- (count it) 3))))
                              set)]
  (spit "src/cljsc2/cljs/material_ui.cljs"
        (str `(~'ns cljsc2.cljs.material_ui (:require ~'[react :as react]
                                                      ~'[fulcro.client.dom :as dom]
                                                      ["@material-ui/core" :as ~'mcore :refer ~(into [] (map symbol all-material-names))]
                                                      ~'[goog.object :as gobj]))
             "\n\n"
             ;;from https://github.com/thheller/shadow/blob/4b6801e5c31458cdc5f85d6fa6d335c9360eb781/src/main/shadow/markup/react/impl/interop.cljs
             (str '(def ^{:private true} element-marker
                     (-> (react/createElement "div" nil)
                         (gobj/get "$$typeof")))

                  "\n\n"
                  '(defn element? [x]
                     (and (object? x)
                          (= element-marker (gobj/get x "$$typeof"))))
                  "\n\n"

                  '(defn convert-props [props]
                     (cond
                       (nil? props)
                       (clj->js {})
                       (map? props)
                       (clj->js props)
                       :else
                       props))
                  "\n\n"

                  '(defn create-element* [arr]
                     {:pre [(array? arr)]}
                     (.apply react/createElement nil arr))
                  "\n\n"
                  '(defn arr-append* [arr x]
                     (.push arr x)
                     arr)
                  "\n\n"
                  '(defn arr-append [arr tail]
                     (reduce arr-append* arr tail))
                  "\n\n"
                  '(defn create-element [type]
                     (fn [& args]
                       (let [[head & tail] args]
                         (cond
                           (map? head)
                           (create-element*
                            (doto (clj->js [type (convert-props head)])
                              (arr-append tail)))

                           (nil? head)
                           (create-element*
                            (doto  (clj->js [type nil])
                              (arr-append tail)))

                           (element? head)
                           (create-element*
                            (doto (clj->js [type nil])
                              (arr-append args)))

                           (object? head)
                           (create-element*
                            (doto (clj->js [type head])
                              (arr-append tail)))

                           :else
                           (create-element*
                            (doto (clj->js [type nil])
                              (arr-append args)))
                           )))))
             "\n\n"
             (->> all-material-names
                  (map (fn [c] (str `(def ~(symbol (str "ui-" (camel->lisp (symbol c)))) (~'create-element ~(symbol c) )))))
                  (clojure.string/join "\n\n")))))

#_'("FormControl" "Zoom" "RootRef" "IconButton" "SelectInput" "Select" "BottomNavigation" "Icon" "ListItemAvatar" "ModalManager" "Modal" "StepIcon" "StepPositionIcon" "InputLabel" "TableBody" "CardContent" "GridListTileBar" "ExpansionPanelDetails" "StepConnector" "CardHeader" "ClickAwayListener" "LinearProgress" "FormControlLabel" "Grid" "Badge" "CircularProgress" "SelectInput" "Select" "BottomNavigation" "Icon" "ListItemAvatar" "ModalManager" "Modal" "StepIcon" "StepPositionIcon" "InputLabel" "TableBody" "CardContent" "GridListTileBar" "ExpansionPanelDetails" "StepConnector" "CardHeader" "ClickAwayListener" "LinearProgress" "FormControlLabel" "Grid" "Badge" "CircularProgress" "RootRef" "IconButton" "RadioGroup" "Paper" "Tabs" "ScrollbarSize" "TabIndicator" "TabScrollButton" "ListSubheader" "AppBar" "ExpansionPanel" "GridListTile" "BottomNavigationAction" "TableFooter" "Drawer" "Tooltip" "MobileStepper" "Portal" "Radio" "Card" "Backdrop" "Slide" "Hidden" "HiddenJs" "HiddenCss" "TableCell" "TableRow" "TableHead" "CardActions" "ExpansionPanelActions" "Textarea" "Input" "DialogTitle" "Snackbar" "StepContent" "Ripple" "ButtonBase" "TouchRipple" "FormGroup" "Chip" "Fade" "Toolbar" "MenuList" "ListItemSecondaryAction" "ArrowDropDown" "KeyboardArrowRight" "Warning" "CheckCircle" "RadioButtonUnchecked" "Cancel" "RadioButtonChecked" "ArrowDownward" "KeyboardArrowLeft" "CheckBox" "CheckBoxOutlineBlank" "IndeterminateCheckBox" "SwitchBase" "Checkbox" "ListItemIcon" "TablePaginationActions" "Step" "TextField" "SwipeableDrawer" "SwipeArea" "SvgIcon" "Stepper" "MenuItem" "Grow" "Collapse" "DialogActions" "Tab" "FormLabel" "SnackbarContent" "ExpansionPanelSummary" "StepButton" "ListItemText" "ListItem" "Dialog" "Popover" "TableSortLabel" "MuiThemeProvider" "TablePagination" "GridList" "NativeSelectInput" "NativeSelect" "DialogContentText" "CardMedia" "Button" "Table" "CssBaseline" "InputAdornment" "List" "Divider" "FormHelperText" "Menu" "DialogContent" "StepLabel" "Avatar" "Typography" "Switch" "RadioGroup" "Paper" "Tabs" "ScrollbarSize" "TabIndicator" "TabScrollButton" "ListSubheader" "AppBar" "ExpansionPanel" "GridListTile" "BottomNavigationAction" "TableFooter" "Drawer" "Tooltip" "MobileStepper" "Portal" "Radio" "Card" "Backdrop" "Slide" "Hidden" "HiddenJs" "HiddenCss" "TableCell" "TableRow" "TableHead" "CardActions" "ExpansionPanelActions" "Textarea" "Input" "DialogTitle" "Snackbar" "StepContent" "Ripple" "ButtonBase" "TouchRipple" "FormGroup" "Chip" "Fade" "Toolbar" "MenuList" "ListItemSecondaryAction" "ArrowDropDown" "KeyboardArrowRight" "Warning" "CheckCircle" "RadioButtonUnchecked" "Cancel" "RadioButtonChecked" "ArrowDownward" "KeyboardArrowLeft" "CheckBox" "CheckBoxOutlineBlank" "IndeterminateCheckBox" "SwitchBase" "Checkbox" "ListItemIcon" "TablePaginationActions" "Step" "TextField" "SwipeableDrawer" "SwipeArea" "SvgIcon" "Stepper" "MenuItem" "Grow" "Collapse" "DialogActions" "Tab" "FormLabel" "SnackbarContent" "ExpansionPanelSummary" "StepButton" "ListItemText" "ListItem" "Dialog" "Popover" "TableSortLabel" "MuiThemeProvider" "TablePagination" "GridList" "NativeSelectInput" "NativeSelect" "DialogContentText" "CardMedia" "Button" "Table" "CssBaseline" "InputAdornment" "List" "Divider" "FormHelperText" "Menu" "DialogContent" "StepLabel" "Avatar" "Typography" "Switch" "FormControl" "Zoom" )
