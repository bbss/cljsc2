(ns cljsc2.cljs.material_ui (:require [react :as react] [fulcro.client.dom :as dom] ["@material-ui/core" :as mcore :refer [IconButton SnackbarContent Switch RootRef StepButton Tooltip StepIcon Typography InputAdornment ListSubheader Button CssBaseline Popover StepConnector SvgIcon Checkbox Radio TablePagination DialogTitle Chip CircularProgress MenuList Modal Paper ClickAwayListener Collapse CardContent Tab Grid ExpansionPanel GridListTileBar Card GridList ListItemText BottomNavigationAction Step ListItemAvatar Hidden Slide NativeSelect Menu Icon Stepper Tabs TableSortLabel MenuItem Divider StepLabel FormControlLabel ListItemIcon Input Badge Dialog ListItemSecondaryAction Zoom ExpansionPanelDetails DialogContent Drawer DialogActions TableBody TableRow FormLabel Portal Toolbar List DialogContentText CardHeader Backdrop TableCell ExpansionPanelActions BottomNavigation AppBar ExpansionPanelSummary StepContent ButtonBase Snackbar SwipeableDrawer TablePaginationActions RadioGroup Table InputLabel Select TableHead GridListTile CardActions MobileStepper TextField ListItem LinearProgress Grow TableFooter FormHelperText CardMedia FormControl Avatar FormGroup Fade]] [goog.object :as gobj]))

(def element-marker (-> (react/createElement "div" nil) (gobj/get "$$typeof")))

(defn element? [x] (and (object? x) (= element-marker (gobj/get x "$$typeof"))))

(defn convert-props [props] (cond (nil? props) (clj->js {}) (map? props) (clj->js props) :else props))

(defn create-element* [arr] {:pre [(array? arr)]} (.apply react/createElement nil arr))

(defn arr-append* [arr x] (.push arr x) arr)

(defn arr-append [arr tail] (reduce arr-append* arr tail))

(defn create-element [type] (fn [& args] (let [[head & tail] args] (cond (map? head) (create-element* (doto (clj->js [type (convert-props head)]) (arr-append tail))) (nil? head) (create-element* (doto (clj->js [type nil]) (arr-append tail))) (element? head) (create-element* (doto (clj->js [type nil]) (arr-append args))) (object? head) (create-element* (doto (clj->js [type head]) (arr-append tail))) :else (create-element* (doto (clj->js [type nil]) (arr-append args)))))))

(def ui-icon-button (create-element IconButton))

(def ui-snackbar-content (create-element SnackbarContent))

(def ui-switch (create-element Switch))

(def ui-root-ref (create-element RootRef))

(def ui-step-button (create-element StepButton))

(def ui-tooltip (create-element Tooltip))

(def ui-step-icon (create-element StepIcon))

(def ui-typography (create-element Typography))

(def ui-input-adornment (create-element InputAdornment))

(def ui-list-subheader (create-element ListSubheader))

(def ui-button (create-element Button))

(def ui-css-baseline (create-element CssBaseline))

(def ui-popover (create-element Popover))

(def ui-step-connector (create-element StepConnector))

(def ui-svg-icon (create-element SvgIcon))

(def ui-checkbox (create-element Checkbox))

(def ui-radio (create-element Radio))

(def ui-table-pagination (create-element TablePagination))

(def ui-dialog-title (create-element DialogTitle))

(def ui-chip (create-element Chip))

(def ui-circular-progress (create-element CircularProgress))

(def ui-menu-list (create-element MenuList))

(def ui-modal (create-element Modal))

(def ui-paper (create-element Paper))

(def ui-click-away-listener (create-element ClickAwayListener))

(def ui-collapse (create-element Collapse))

(def ui-card-content (create-element CardContent))

(def ui-tab (create-element Tab))

(def ui-grid (create-element Grid))

(def ui-expansion-panel (create-element ExpansionPanel))

(def ui-grid-list-tile-bar (create-element GridListTileBar))

(def ui-card (create-element Card))

(def ui-grid-list (create-element GridList))

(def ui-list-item-text (create-element ListItemText))

(def ui-bottom-navigation-action (create-element BottomNavigationAction))

(def ui-step (create-element Step))

(def ui-list-item-avatar (create-element ListItemAvatar))

(def ui-hidden (create-element Hidden))

(def ui-slide (create-element Slide))

(def ui-native-select (create-element NativeSelect))

(def ui-menu (create-element Menu))

(def ui-icon (create-element Icon))

(def ui-stepper (create-element Stepper))

(def ui-tabs (create-element Tabs))

(def ui-table-sort-label (create-element TableSortLabel))

(def ui-menu-item (create-element MenuItem))

(def ui-divider (create-element Divider))

(def ui-step-label (create-element StepLabel))

(def ui-form-control-label (create-element FormControlLabel))

(def ui-list-item-icon (create-element ListItemIcon))

(def ui-input (create-element Input))

(def ui-badge (create-element Badge))

(def ui-dialog (create-element Dialog))

(def ui-list-item-secondary-action (create-element ListItemSecondaryAction))

(def ui-zoom (create-element Zoom))

(def ui-expansion-panel-details (create-element ExpansionPanelDetails))

(def ui-dialog-content (create-element DialogContent))

(def ui-drawer (create-element Drawer))

(def ui-dialog-actions (create-element DialogActions))

(def ui-table-body (create-element TableBody))

(def ui-table-row (create-element TableRow))

(def ui-form-label (create-element FormLabel))

(def ui-portal (create-element Portal))

(def ui-toolbar (create-element Toolbar))

(def ui-list (create-element List))

(def ui-dialog-content-text (create-element DialogContentText))

(def ui-card-header (create-element CardHeader))

(def ui-backdrop (create-element Backdrop))

(def ui-table-cell (create-element TableCell))

(def ui-expansion-panel-actions (create-element ExpansionPanelActions))

(def ui-bottom-navigation (create-element BottomNavigation))

(def ui-app-bar (create-element AppBar))

(def ui-expansion-panel-summary (create-element ExpansionPanelSummary))

(def ui-step-content (create-element StepContent))

(def ui-button-base (create-element ButtonBase))

(def ui-snackbar (create-element Snackbar))

(def ui-swipeable-drawer (create-element SwipeableDrawer))

(def ui-table-pagination-actions (create-element TablePaginationActions))

(def ui-radio-group (create-element RadioGroup))

(def ui-table (create-element Table))

(def ui-input-label (create-element InputLabel))

(def ui-select (create-element Select))

(def ui-table-head (create-element TableHead))

(def ui-grid-list-tile (create-element GridListTile))

(def ui-card-actions (create-element CardActions))

(def ui-mobile-stepper (create-element MobileStepper))

(def ui-text-field (create-element TextField))

(def ui-list-item (create-element ListItem))

(def ui-linear-progress (create-element LinearProgress))

(def ui-grow (create-element Grow))

(def ui-table-footer (create-element TableFooter))

(def ui-form-helper-text (create-element FormHelperText))

(def ui-card-media (create-element CardMedia))

(def ui-form-control (create-element FormControl))

(def ui-avatar (create-element Avatar))

(def ui-form-group (create-element FormGroup))

(def ui-fade (create-element Fade))