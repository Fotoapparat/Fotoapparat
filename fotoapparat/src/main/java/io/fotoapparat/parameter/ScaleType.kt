package io.fotoapparat.parameter

/**
 * The scale type of the preview relatively to the view.
 */
enum class ScaleType {

    /**
     * The preview will be scaled so as its one dimensions will be equal and the other one equal or
     * larger than the corresponding dimension of the view
     */
    CenterCrop,

    /**
     * The preview will be scaled so as its one dimensions will be equal and the other one equal or
     * smaller than the corresponding dimension of the view
     */
    CenterInside,


    /**
     * The preview will be scaled so as its one dimensions will be equal and the other one equal or
     * larger than the corresponding dimension of the view with focus on the top part
     */
    TopCrop,

}