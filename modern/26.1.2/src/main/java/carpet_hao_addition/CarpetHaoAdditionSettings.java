package carpet_hao_addition;

import carpet.api.settings.Rule;

/**
 * Example rule class for Carpet-Hao-Addition.
 * <p>
 * Any static field annotated with {@link Rule} becomes a Carpet rule of this
 * extension's settings manager. They are edited in game with
 * {@code /haoaddition <rule> <value>} (or {@code /haoaddition list}).
 * <p>
 * Rule names and descriptions shown in game come from the translations provided
 * in {@link CarpetHaoAdditionExtension#canHasTranslations(String)}.
 */
public class CarpetHaoAdditionSettings
{
    @Rule(categories = {CarpetHaoAdditionExtension.MANAGER_ID})
    public static boolean exampleBoolean = false;

    @Rule(
            categories = {CarpetHaoAdditionExtension.MANAGER_ID},
            options = {"foo", "bar", "baz"},
            strict = true
    )
    public static String exampleString = "foo";
}
